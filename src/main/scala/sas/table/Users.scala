package sas.table

import slick.jdbc.SQLiteProfile.api._

class Users(tag: Tag) extends Table[(Long, String, String)](tag, "users") {

  def id = column[Long]("id", O.PrimaryKey)

  def username = column[String]("username")

  def hashedPassword = column[String]("hashed_password")

  def * = (id, username, hashedPassword)
}
