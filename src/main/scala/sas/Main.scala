package sas

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import sas.route.User
import slick.jdbc.SQLiteProfile.api._

object Main {

  def main(args: Array[String]): Unit = {
    val db = Database.forURL("jdbc:sqlite:" + args(0))
    val route = new User(db).route
    implicit val system = ActorSystem("cis-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val bindingFuture = Http().bindAndHandle(route, args(1), args(2).toInt)
    sys.addShutdownHook({
      db.close()
    })
  }
}
