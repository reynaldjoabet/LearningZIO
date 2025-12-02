//import com.typesafe.config.ConfigFactory
import org.keycloak.representations.AccessToken
import java.security.KeyFactory
import org.keycloak.jose.jws.AlgorithmType
import java.math.BigInteger
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import org.keycloak.{TokenVerifier => keycloakTV}
import scala.util.Try
import scala.concurrent.duration._
import zio._
import zio.config.*

final case class KeycloakConfig(
  realmUrl: String,
  publicKey: PublicKeyConfig,
  timeoutSeconds: Int,
)

final case class PublicKeyConfig(
  modulus: String,
  exponent: String,
)

object KeycloakConfig {
  // import zio.config.magnolia.DeriveConfigDescriptor.descriptor

  // implicit val keycloakConfigDescriptor: ConfigDescriptor[KeycloakConfig] = descriptor[KeycloakConfig]
  // implicit val publicKeyConfigDescriptor: ConfigDescriptor[PublicKeyConfig] = descriptor[PublicKeyConfig]

  val config: Config[KeycloakConfig] = {
    // Compose the public key config from modulus and exponent
    val pkConfig: Config[PublicKeyConfig] =
      (Config.string("publicKey.modulus") zip Config.string("publicKey.exponent")).map { case (m, e) =>
        PublicKeyConfig(m, e)
      }

    // Compose the full KeycloakConfig from realmUrl, publicKey and timeout
    (Config.string("realmUrl").withDefault("http://localhost:8080/auth/realms/myrealm") zip pkConfig zip
      Config.int("timeoutSeconds").withDefault(5)).map { case (realm, pk, timeout) =>
      KeycloakConfig(realm, pk, timeout)
    }
  }
}

/** Small service interface */
trait TokenVerifier {
  def verifyToken(token: String): Task[AccessToken]
}

object TokenVerifier {
  def verifyToken(token: String): ZIO[TokenVerifier, Throwable, AccessToken] =
    ZIO.serviceWithZIO[TokenVerifier](_.verifyToken(token))
}

/** ZIO-friendly Keycloak implementation */
final class KeycloakTokenVerifierImpl(
  realmUrl: String,
  publicKey: java.security.PublicKey,
  timeout: Int,
) extends TokenVerifier {

  // perform verification on blocking pool because it may perform CPU/crypto work
  override def verifyToken(token: String): Task[AccessToken] =
    ZIO.attemptBlocking {
      val tokenVerifier = keycloakTV.create(token, classOf[AccessToken])
      // configure verifier (same as original)
      tokenVerifier.withDefaultChecks()
      tokenVerifier.realmUrl(realmUrl)
      tokenVerifier.publicKey(publicKey).verify().getToken
    }.timeoutFail(new RuntimeException("token verification timed out"))(zio.Duration.fromSeconds(timeout))

}

object KeycloakTokenVerifierImpl {

  val tokenVerifierLayer: ZLayer[KeycloakConfig, Throwable, TokenVerifier] =
    ZLayer.fromZIO {
      for {
        keycloakConfig <- ZIO.service[KeycloakConfig]
        realmUrl        = keycloakConfig.realmUrl

        keyFactory = KeyFactory.getInstance(AlgorithmType.RSA.toString)
        urlDecoder = Base64.getUrlDecoder
        modulus    = new BigInteger(1, urlDecoder.decode(keycloakConfig.publicKey.modulus))
        exponent   = new BigInteger(1, urlDecoder.decode(keycloakConfig.publicKey.exponent))
        publicKey  = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent))

      } yield new KeycloakTokenVerifierImpl(realmUrl, publicKey, keycloakConfig.timeoutSeconds)
    }

  val layer =
    ZLayer.fromZIO {
      for {
        keycloakConfig <- ZIO.config(KeycloakConfig.config)
        realmUrl        = keycloakConfig.realmUrl

        keyFactory = KeyFactory.getInstance(AlgorithmType.RSA.toString)
        urlDecoder = Base64.getUrlDecoder
        modulus    = new BigInteger(1, urlDecoder.decode(keycloakConfig.publicKey.modulus))
        exponent   = new BigInteger(1, urlDecoder.decode(keycloakConfig.publicKey.exponent))
        publicKey  = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent))

      } yield new KeycloakTokenVerifierImpl(realmUrl, publicKey, keycloakConfig.timeoutSeconds)
    }
}
