package sas.table

import slick.jdbc.SQLiteProfile.api._

class Downloads(tag: Tag) extends Table[(Long, Long, Long, Long, Long)](tag, "downloads") {

  def id = column[Long]("id", O.PrimaryKey)

  def appId = column[Long]("app_id")

  def userId = column[Long]("user_id")

  def count = column[Long]("count")

  def price = column[Long]("price")

  def * = (id, appId, userId, count, price)

  def app = foreignKey("app", appId, apps)(_.id)

  def user = foreignKey("user", userId, users)(_.id)
}
