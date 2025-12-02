package object authenticator {
  val ANSW_EMAIL_CODE = "user.attributes.code"

  // Configurable fields
  val CODE_ACTIVATIONDELAYINSEC = "CODE.ACTIVATIONDELAYINSEC"
  val CODE_VALIDINMIN           = "CODE.VALIDINMIN"
  val API_URL                   = "API.URL"

  // email spec codes
  val AUTH_NOTE_USER_EMAIL = "user-email"
  val AUTH_NOTE_EMAIL_CODE = "email-code"
  val AUTH_NOTE_TIMESTAMP  = "timestamp"

  final case class EmailConfig(
    codeActivationDelayInSec: Int,
    codeValidInMin: Int,
    apiUrl: String,
  )
}
