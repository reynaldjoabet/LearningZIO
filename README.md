# LearningZIO

ZIO Schema is a library by ZIO that lets you treat type structure (Scala case classes, sealed traits, etc.) as first-class values (schemas)


you define your Scala types (and derive a Schema[A]), and then you can automatically obtain a BinaryCodec[A] that serializes/deserializes to/from Protobuf bytes.


ZIO Logging is simple logging for ZIO apps, with correlation, context, and pluggable backends out of the box with integrations for common logging backends

Pluggable Backends — Support multiple backends like ZIO Console, SLF4j, JPL.

Logger Context — It has a first citizen Logger Context implemented on top of FiberRef. The Logger Context maintains information like logger name, filters, correlation id, and so forth across different fibers. It supports Mapped Diagnostic Context (MDC) which manages contextual information across fibers in a concurrent environment.

Composable — Loggers are composable together via contraMap.




Red Hat single sign-on (SSO)—or its open source version, Keycloak—is one of the leading products for web SSO capabilities, and is based on popular standards such as Security Assertion Markup Language (SAML) 2.0, OpenID Connect, and OAuth 2.0. One of Red Hat SSO's strongest features is that we can access Keycloak directly in many ways, whether through a simple HTML login form, or an API call.

[better-auth-with-tokens-and-keycloak](https://hari-krishna-tech.github.io/2025/02/28/better-auth-with-tokens-and-keycloak/)