// package authenticator

// import zio._
// import scala.jdk.CollectionConverters._
// import org.keycloak.models.{UserModel, AuthenticatorConfigModel}

// object AuthenticatorUtil {

//   /** Safely read first attribute value from a Keycloak UserModel.
//     *
//     * Returns UIO[Option[String]] — never fails, may be None if user or attribute missing.
//     */
//   def getAttributeValue(user: UserModel, attributeName: String): UIO[Option[String]] =
//     ZIO.succeed {
//       Option(user)
//         .flatMap(u => Option(u.getFirstAttribute(attributeName)))
//         .filter(_.nonEmpty)
//     }

//   /** Read config map safely and return Option[String].
//     *
//     * The underlying AuthenticatorConfigModel#getConfig may be null; values in the map may be null as well.
//     * This returns None if config or key is missing or blank.
//     */
//   def getConfigString(config: AuthenticatorConfigModel, configName: String): UIO[Option[String]] =
//     ZIO.succeed {
//       Option(config)
//         .flatMap(c => Option(c.getConfig))              // config map may be null
//         .flatMap(m => Option(m.get(configName)))        // value may be null
//         .map(_.trim)
//         .filter(_.nonEmpty)
//     }

//   /** Read config string with a default value (pure, non-throwing). */
//   def getConfigStringOr(config: AuthenticatorConfigModel, configName: String, default: String): UIO[String] =
//     getConfigString(config, configName).map(_.getOrElse(default))

//   /** Parse a Long from config value if present.
//     *
//     * Returns Task[Option[Long]] — logs and returns None on parse failure.
//     * If you prefer a pure option without logging, use `parseConfigLongPure` below.
//     */
//   def getConfigLong(config: AuthenticatorConfigModel, configName: String): Task[Option[Long]] =
//     getConfigString(config, configName).flatMap {
//       case None => ZIO.succeed(None)
//       case Some(s) =>
//         ZIO.attempt(s.toLong).foldZIO(
//           { ex =>
//             // Log the parsing error and return None (mirrors original behavior which logged and returned null)
//             ZIO.logError(s"Cannot convert config value '$s' for key '$configName' to Long: ${ex.getMessage}") *> ZIO.succeed(None)
//           },
//           l => ZIO.succeed(Some(l))
//         )
//     }

//   /** Like getConfigLong but accepts a default. */
//   def getConfigLongOr(config: AuthenticatorConfigModel, configName: String, default: Long): Task[Long] =
//     getConfigLong(config, configName).map(_.getOrElse(default))

//   /** Pure variant - no logging (returns Option[Long]) */
//   def parseConfigLongPure(config: AuthenticatorConfigModel, configName: String): UIO[Option[Long]] =
//     getConfigString(config, configName).map(_.flatMap(s => scala.util.Try(s.toLong).toOption))

//   // Convenience overloads to interop with Java-style callers (if desired)
//   /** Legacy-compatible APIs returning nullables (discouraged) */
//   def getAttributeValueNullable(user: UserModel, attributeName: String): UIO[String] =
//     getAttributeValue(user, attributeName).map(_.orNull)

//   def getConfigStringNullable(config: AuthenticatorConfigModel, configName: String): UIO[String] =
//     getConfigString(config, configName).map(_.orNull)

//   def getConfigLongNullable(config: AuthenticatorConfigModel, configName: String): Task[java.lang.Long] =
//     getConfigLong(config, configName).map(_.map(java.lang.Long.valueOf).orNull)
// }