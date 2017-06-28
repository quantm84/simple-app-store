package sas.json

import spray.json.DefaultJsonProtocol._

case class Error(code: Int, message: String)

object Error {
  def duplicatedUser(): Error = Error(-1, "duplicated username")

  def wrongCredentials(): Error = Error(-2, "wrong credentials")
}

object ErrorProtocol {
  implicit val errorFormat = jsonFormat2(Error.apply)
}
