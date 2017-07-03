package sas.json

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class SASError(code: Int, message: String)

object SASError {
  val DuplicatedUser: SASError = SASError(-1, "duplicated username")

  val WrongCredentials: SASError = SASError(-2, "wrong credentials")

  val UserNotFound: SASError = SASError(-3, "user not found")

  val MalformedAuthorization: SASError = SASError(-4, "malformed authorization string")

  val AuthorizationFailed: SASError = SASError(-5, "authorization failed")

  val AppNotFound: SASError = SASError(-6, "app not found")
}

object SASErrorProtocol {
  implicit val errorFormat: RootJsonFormat[SASError] = jsonFormat2(SASError.apply)
}
