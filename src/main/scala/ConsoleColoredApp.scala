// package zio.logging.example

// import zio.config.typesafe.TypesafeConfigProvider
// import zio.logging.consoleLogger
// import zio.{ Cause, Config, ConfigProvider, ExitCode, Runtime, Scope, URIO, ZIO, ZIOAppDefault, ZLayer }

// object ConsoleColoredApp extends ZIOAppDefault {

//   val configString: String =
//     s"""
//        |logger {
//        |
//        |  format = "%highlight{%timestamp{yyyy-MM-dd'T'HH:mm:ssZ} %fixed{7}{%level} [%fiberId] %name:%line %message %cause}"
//        |
//        |  filter {
//        |    mappings {
//        |      "zio.logging.example.LivePingService" = "DEBUG"
//        |    }
//        |  }
//        |}
//        |""".stripMargin

//   val configProvider: ConfigProvider = TypesafeConfigProvider.fromHoconString(configString)

//   override val bootstrap: ZLayer[Any, Config.Error, Unit] =
//     Runtime.removeDefaultLoggers >>> Runtime.setConfigProvider(configProvider) >>> consoleLogger()

//   private def ping(address: String): URIO[PingService, Unit] =
//     PingService
//       .ping(address)
//       .foldZIO(
//         e => ZIO.logErrorCause(s"ping: $address - error", Cause.fail(e)),
//         r => ZIO.logInfo(s"ping: $address - result: $r")
//       )

//   override def run: ZIO[Scope, Any, ExitCode] =
//     (for {
//       _ <- ping("127.0.0.1")
//       _ <- ping("x8.8.8.8")
//     } yield ExitCode.success).provide(LivePingService.layer)

// }
