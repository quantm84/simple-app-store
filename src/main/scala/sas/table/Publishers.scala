package sas.table

import slick.jdbc.SQLiteProfile.api._

class Publishers(tag: Tag) extends Table[(Long, String, String)](tag, "publishers") {

  def id = column[Long]("id", O.PrimaryKey)

  def username = column[String]("username")

  def hashedPassword = column[String]("hashed_password")

  def * = (id, username, hashedPassword)
}
