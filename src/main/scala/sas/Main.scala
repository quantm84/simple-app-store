package sas

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import sas.route.User
import sas.table._
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {
    val dbPath = args(0)
    val host = args(1)
    val port = args(2).toInt
    val db = Database.forURL("jdbc:sqlite:" + dbPath)
    if (!new File(dbPath).exists()) {
      Await.ready(db.run(setup), Duration.Inf)
    }
    val route = new User(db).route
    implicit val system = ActorSystem("sas-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val bindingFuture = Http().bindAndHandle(route, host, port)
    sys.addShutdownHook({
      Await.result(bindingFuture.map(_.unbind()), Duration.Inf)
      Await.result(system.terminate(), Duration.Inf)
      db.close()
    })
  }
}
