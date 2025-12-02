// package zio.logging.example

// import zio.config.typesafe.TypesafeConfigProvider
// import zio.logging.fileLogger
// import zio.logging.backend.SLF4J
// import zio.{Config, ConfigProvider, ExitCode, Runtime, Scope, ZIO, ZIOAppDefault, ZLayer}

// object FileApp extends ZIOAppDefault {

//   val configString: String =
//     s"""
//        |logger {
//        |  format = "%timestamp{yyyy-MM-dd'T'HH:mm:ssZ} %fixed{7}{%level} [%fiberId] %name:%line %message %cause"
//        |  path = "file:///tmp/file_app.log"
//        |  rollingPolicy {
//        |    type = TimeBasedRollingPolicy
//        |  }
//        |}
//        |""".stripMargin

//   val configProvider: ConfigProvider = TypesafeConfigProvider.fromHoconString(configString)

//   override val bootstrap: ZLayer[Any, Config.Error, Unit] =
//     Runtime.removeDefaultLoggers >>> Runtime.setConfigProvider(configProvider) >>> fileLogger()

//   override def run: ZIO[Scope, Any, ExitCode] =
//     for {
//       _ <- ZIO.logInfo("Start")
//       _ <- ZIO.fail("FAILURE")
//       _ <- ZIO.logInfo("Done")
//     } yield ExitCode.success
// }
