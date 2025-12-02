import zio.ZIOAppDefault
import zio.ZLayer
import zio.ZIOAppArgs
import zio.logging.backend.SLF4J
import zio.logging.*
import zio.*
import zio.logging.LogFormat

object Main extends ZIOAppDefault {

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {

    val app: ZIO[TokenVerifier, Throwable, Unit] =
      for {
        token <- ZIO.succeed("ey...") // token from header
        at    <- TokenVerifier.verifyToken(token)
        _     <- ZIO.debug(s"token subject: ${at.getSubject}")
      } yield ()

    app.provideLayer(KeycloakTokenVerifierImpl.layer)

  }

  // override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = SLF4J.slf4j(LogFormat.default)
}
