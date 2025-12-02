import zio._
import zio.logging.*
import jakarta.mail._
import jakarta.mail.internet._
import java.util.Properties
import java.io.File
import java.nio.file.Path
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import zio.logging.LogFormat.*
import org.apache.commons.logging.Log
import zio.logging.LoggerNameExtractor
import zio.logging.ConsoleLoggerConfig
//import zio.logging.backend.console.{ConsoleLoggerConfig, consoleJsonLogger, consoleLogger, logFilter}
case class SMTPConfig(
  smtpServer: String,
  port: Int,
  username: String,
  password: String,
  fromAddress: String,
  useStartTls: Boolean = true,
  useSsl: Boolean = false, // true when using port 465/SMTPS
  connectionTimeoutMs: Int = 10000,
  readTimeoutMs: Int = 10000,
  maxRetries: Int = 3,
  retryBaseDelay: scala.concurrent.duration.FiniteDuration = 200.millis,
  maxConcurrentSends: Int = 10,
)

case class Email(
  to: List[String],
  cc: List[String] = Nil,
  bcc: List[String] = Nil,
  subject: String,
  plainText: Option[String] = None,
  html: Option[String] = None,
  attachments: List[Path] = Nil, // optional attachments
)

trait EmailService {
  def send(email: Email): Task[Unit]
}

final class SmtpEmailService(
  config: SMTPConfig,
  session: Session,
  sendSemaphore: Semaphore,
) extends EmailService {

  // Helper: create message object
  private def buildMessage(email: Email): Task[MimeMessage] = ZIO.attempt {
    val msg = new MimeMessage(session)
    msg.setFrom(new InternetAddress(config.fromAddress))

    def addRecipients(kind: Message.RecipientType, list: List[String]): Unit =
      if (list.nonEmpty)
        msg.setRecipients(
          kind,
          list.map(new InternetAddress(_)).toArray.asInstanceOf[Array[Address]],
        )

    addRecipients(Message.RecipientType.TO, email.to)
    addRecipients(Message.RecipientType.CC, email.cc)
    addRecipients(Message.RecipientType.BCC, email.bcc)

    msg.setSubject(email.subject)

    // Build body (plain + html + attachments)
    val needsMultipart =
      email.attachments.nonEmpty || (email.plainText.isDefined && email.html.isDefined)

    if (!needsMultipart && email.html.isDefined) {
      // simple HTML-only message
      msg.setContent(email.html.getOrElse(""), "text/html; charset=UTF-8")
    } else if (!needsMultipart && email.plainText.isDefined) {
      msg.setText(email.plainText.getOrElse(""), "utf-8")
    } else {
      // multipart alternative (plain + html) + attachments
      val root = new MimeMultipart() // "mixed" by default

      // If both plain and html, create "alternative" part
      (email.plainText, email.html) match {
        case (Some(pt), Some(html)) =>
          val alt       = new MimeMultipart("alternative")
          val plainPart = new MimeBodyPart()
          plainPart.setText(pt, "utf-8")
          val htmlPart = new MimeBodyPart()
          htmlPart.setContent(html, "text/html; charset=UTF-8")
          alt.addBodyPart(plainPart)
          alt.addBodyPart(htmlPart)

          val altWrapper = new MimeBodyPart()
          altWrapper.setContent(alt)
          root.addBodyPart(altWrapper)

        case (Some(pt), None) =>
          val plainPart = new MimeBodyPart()
          plainPart.setText(pt, "utf-8")
          root.addBodyPart(plainPart)

        case (None, Some(html)) =>
          val htmlPart = new MimeBodyPart()
          htmlPart.setContent(html, "text/html; charset=UTF-8")
          root.addBodyPart(htmlPart)

        case _ =>
        // empty body
      }

      // attachments
      email.attachments.foreach { p =>
        val attachPart = new MimeBodyPart()
        val file       = p.toFile
        attachPart.attachFile(file)
        attachPart.setFileName(file.getName)
        root.addBodyPart(attachPart)
      }

      msg.setContent(root)
    }

    msg.saveChanges()
    msg
  }

  // Low-level send that uses JakartaMail Transport. Runs blocking.
  private def sendBlocking(msg: MimeMessage): Task[Unit] = ZIO.attemptBlocking {
    // Use Transport explicitly for better control (connect/close)
    val transport = session.getTransport("smtp")
    try {
      // Connect with credentials
      transport.connect(
        config.smtpServer,
        config.port,
        config.username,
        config.password,
      )
      transport.sendMessage(msg, msg.getAllRecipients)
    } finally {
      try transport.close()
      catch {
        case _: Throwable => /* swallow close errors */
      }
    }
  }

  // Public send with retries, timeout and concurrency control
  override def send(email: Email): Task[Unit] = {
    val op = for {
      _   <- ZIO.logInfo(s"Preparing to send email to ${email.to.mkString(",")}")
      msg <- buildMessage(email)
      _   <- ZIO.logDebug(s"Built message with subject=${email.subject}")
      _   <- sendBlocking(msg)
      _   <- ZIO.logInfo(s"Email sent to ${email.to.mkString(",")}")
    } yield ()

    // concurrency limit + retries with exponential backoff + total timeout
    val retryPolicy =
      Schedule.exponential(
        zio.Duration.fromScala(config.retryBaseDelay),
      ) && Schedule.recurs(config.maxRetries.toLong)

    val timeout =
      (config.connectionTimeoutMs + config.readTimeoutMs).millis * (config.maxRetries + 1)

    sendSemaphore.withPermit {
      op.retry(retryPolicy)
        .timeoutFail(new RuntimeException("send-timed-out"))(
          zio.Duration.fromScala(timeout),
        )
    }.mapError { e =>
      // do not leak secret info in error messages
      new RuntimeException(
        s"Failed to send email to ${email.to.mkString(",")}: ${e.getMessage}",
        e,
      )
    }
  }

  // Send using SMTP and XOAUTH2: some SMTP servers support connecting and issuing "AUTH XOAUTH2 <base64(...)>"
  def sendWithXOAuth2(
    session: Session,
    from: String,
    to: List[String],
    subject: String,
    bodyHtml: String,
    userEmail: String,
    accessToken: String,
  ): Task[Unit] = ZIO.attemptBlocking {
    val msg = new MimeMessage(session)
    msg.setFrom(new InternetAddress(from))
    msg.setRecipients(Message.RecipientType.TO, to.map(new InternetAddress(_)).toArray.asInstanceOf[Array[Address]])
    msg.setSubject(subject)
    msg.setContent(bodyHtml, "text/html; charset=UTF-8")
    msg.saveChanges()

    // Many JakartaMail implementations include com.sun.mail.smtp.SMTPTransport
    val transport = session.getTransport("smtp")
    try {
      transport.connect() // no user/pass
      // Issue XOAUTH2 AUTH command: transport supports method "authenticate" or "issueCommand" via reflection in some clients.
      // The com.sun.mail.smtp.SMTPTransport class has a method: authenticate(String host, String user, String oauthToken)
      // But if not available, can use: transport.connect(host, port, user, accessToken) WITH SASL XOAUTH2 enabled in props
      transport.sendMessage(msg, msg.getAllRecipients)
    } finally transport.close()
  }

}

object SmtpEmailService {

  private val logFilter = LogFilter.LogLevelByNameConfig(
    LogLevel.Info,
    ("org.apache.jena", LogLevel.Debug),
    ("io.netty", LogLevel.Info),
    ("org.ehcache", LogLevel.Info),
    ("zio.http.*", LogLevel.Debug),
    // Uncomment the following lines to change the log level for specific loggers:
    // ("zio.logging.slf4j", LogLevel.Debug)
    // ("SLF4J-LOGGER", LogLevel.Warning)
  )
  private val logFormatText: LogFormat =
    timestamp.fixed(32).color(LogColor.BLUE) |-|
      level.fixed(5).highlight |-|
      line.highlight |-|
      LoggerNameExtractor.loggerNameAnnotationOrTrace.toLogFormat() |-|
      label("annotations", bracketed(annotations)) |-|
      label("spans", bracketed(spans)) +
      (space + label("cause", cause).highlight).filter(LogFilter.causeNonEmpty)

  private val logFormatJson: LogFormat =
    LogFormat.label(
      "name",
      LoggerNameExtractor.loggerNameAnnotationOrTrace.toLogFormat(),
    ) +
      LogFormat.default +
      LogFormat.annotations +
      LogFormat.spans

  private val textLogger: ULayer[Unit] = consoleLogger(
    ConsoleLoggerConfig(logFormatText, logFilter),
  )
  private val jsonLogger: ULayer[Unit] = consoleJsonLogger(
    ConsoleLoggerConfig(logFormatJson, logFilter),
  )
  // Helper to create JavaMail Session from config
  def sessionFromConfig(config: SMTPConfig): Task[Session] = ZIO.attempt {
    val props = new Properties()
    props.put("mail.smtp.host", config.smtpServer)
    props.put("mail.smtp.port", config.port.toString)
    props.put("mail.smtp.auth", "true")
    props.put(
      "mail.smtp.connectiontimeout",
      config.connectionTimeoutMs.toString,
    )
    props.put("mail.smtp.timeout", config.readTimeoutMs.toString)

    if (config.useStartTls) props.put("mail.smtp.starttls.enable", "true")
    if (config.useSsl) {
      props.put("mail.smtp.ssl.enable", "true")
      // some providers require socketFactory settings; prefer starttls where possible
    }

    // Consider: mail.debug for diagnosing (do not enable in prod)
    Session.getInstance(props)
  }

  // Layer to provide EmailService with concurrency limit and logger
  def layer(
    config: SMTPConfig,
  ) =
    ZLayer.scoped {
      for {

        session <- sessionFromConfig(config).orDie
        sem     <- Semaphore.make(config.maxConcurrentSends.toLong)
        svc      = new SmtpEmailService(config, session, sem)
        _       <- ZIO.acquireRelease(ZIO.unit)(_ => ZIO.unit) // placeholder if you need cleanup
      } yield svc
    }
}
