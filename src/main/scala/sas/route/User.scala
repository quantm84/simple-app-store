package sas.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import sas.json.Error
import sas.json.ErrorProtocol._
import sas.table.users
import slick.jdbc.SQLiteProfile.api._
import spray.json.DefaultJsonProtocol._

class User(db: Database) {

  private val register = path("register") {
    post {
      parameters('username, 'hashed_password) { (u, p) =>
        val action = users.filter(_.username === u).result
        onSuccess(db.run(action)) { r =>
          if (r.isEmpty) {
            val userIdAction = (users returning users.map(_.id)) += (0, u, p)
            onSuccess(db.run(userIdAction)) { userId =>
              complete(Map("user_id" -> userId))
            }
          } else {
            complete(Error.duplicatedUser())
          }
        }
      }
    }
  }

  private val login = path("login") {
    post {
      parameters('username, 'hashed_password) { (u, p) =>
        val action = users.filter(user => user.username === u && user.hashedPassword === p).result
        onSuccess(db.run(action)) { r =>
          if (r.isEmpty) {
            complete(Error.wrongCredentials())
          } else {
            complete(Map("user_id" -> r.head._1))
          }
        }
      }
    }
  }

  val route = pathPrefix("user") {
    register ~
      login
  }

}
