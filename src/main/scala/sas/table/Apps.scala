package sas.table

import slick.jdbc.SQLiteProfile.api._

class Apps(tag: Tag) extends Table[(Long, String, String, String, Long, Long, Long, Long)](tag, "apps") {

  def id = column[Long]("id", O.PrimaryKey)

  def name = column[String]("name")

  def description = column[String]("description")

  def location = column[String]("location")

  def price = column[Long]("price")

  def numDownloads = column[Long]("num_downloads")

  def uploadMillis = column[Long]("upload_millis")

  def publisherId = column[Long]("publisher_id")

  def * = (id, name, description, location, price, numDownloads, uploadMillis, publisherId)

  def publisher = foreignKey("publisher", publisherId, publishers)(_.id)
}
