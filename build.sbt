import Dependencies._

ThisBuild / scalaVersion := "3.3.6"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "LearningZIO",
    libraryDependencies ++= Seq(
      munit          % Test,
      "dev.zio"     %% "zio-opentracing"             % "3.1.11",
      "dev.zio"     %% "zio-aws-kinesis"             % "7.37.3.1",
      "dev.zio"     %% "zio-aws-cloudwatch"          % "7.37.3.1",
      "dev.zio"     %% "zio-schema-protobuf"         % "1.7.5",
      "dev.zio"     %% "zio-json-golden"             % "0.7.45",
      "dev.zio"     %% "zio-schema-derivation"       % "1.7.5",
      "dev.zio"     %% "zio-logging-slf4j"           % "2.5.1",
      "dev.zio"     %% "zio-schema-json"             % "1.7.5",
      "dev.zio"     %% "zio-aws-netty"               % "7.37.3.1",
      "dev.zio"     %% "zio-kafka"                   % "3.1.0",
      "dev.zio"     %% "zio-aws-dynamodb"            % "7.37.3.1",
      "dev.zio"     %% "zio-test-magnolia"           % "2.1.22" % Test,
      "dev.zio"     %% "zio-logging"                 % "2.5.1",
      "dev.zio"     %% "zio-config"                  % "4.0.5",
      "dev.zio"     %% "zio-schema"                  % "1.7.5",
      "dev.zio"     %% "zio-http"                    % "3.5.1",
      "dev.zio"     %% "zio-config-magnolia"         % "4.0.5",
      "dev.zio"     %% "zio-config"                  % "4.0.5",
      "dev.zio"     %% "zio-json"                    % "0.7.45",
      "dev.zio"     %% "zio"                         % "2.1.22",
      "dev.zio"     %% "zio-test"                    % "2.1.22" % Test,
      "dev.zio"     %% "zio-test-sbt"                % "2.1.22" % Test,
      "dev.zio"     %% "zio-aws-core"                % "7.37.3.1",
      "jakarta.mail" % "jakarta.mail-api"            % "2.1.5",
      "org.keycloak" % "keycloak-core"               % "26.4.4",
      "org.keycloak" % "keycloak-admin-client"       % "26.0.7",
      "com.yubico"   % "webauthn-server-core"        % "2.7.0",
      "com.yubico"   % "webauthn-server-attestation" % "2.7.0",
    ),
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
