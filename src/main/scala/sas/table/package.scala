package sas

import slick.jdbc.SQLiteProfile.api._

package object table {
  lazy val apps = TableQuery[Apps]
  lazy val downloads = TableQuery[Downloads]
  lazy val publishers = TableQuery[Publishers]
  lazy val users = TableQuery[Users]

  lazy val setup = DBIO.seq(
    (apps.schema ++ downloads.schema ++ publishers.schema ++ users.schema).create
  )
}
