package sas.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{Authorization, Date}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import sas.AppStorage
import sas.json.SASError._
import sas.json.SASErrorProtocol._
import sas.table.{apps, users}
import sas.util.{Crypto, SASException}
import slick.jdbc.SQLiteProfile.api._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class User(db: Database, appStorage: AppStorage) {

  private def verify(publicKey: String, authorization: String): Future[Long] = {
    val pair = parseAuthorization(authorization)
    if (pair.isFailure) {
      return Future.failed(SASException(MalformedAuthorization))
    }
    val userId = pair.get._1
    val signature = pair.get._2
    val action = users
      .filter(_.id === userId)
      .map(_.hashedPassword)
      .result
    db.run(action)
      .map { seq =>
        if (seq.isEmpty) {
          throw SASException(UserNotFound)
        } else if (signature == Crypto.base64hmacsha1(publicKey, seq.head)) {
          userId
        } else {
          throw SASException(AuthorizationFailed)
        }
      }
  }

  private def parseAuthorization(authorization: String): Try[(Long, String)] = Try({
    val p1 = authorization.indexOf(' ')
    val p2 = authorization.indexOf(':')
    (authorization.substring(p1 + 1, p2).toLong, authorization.substring(p2 + 1))
  })

  private def doComplete[T](future: Future[T])(implicit format: RootJsonFormat[T]) = onComplete(future) {
    case Success(map) => complete(map)
    case Failure(t) => handle(t)
  }

  private def handle(t: Throwable): Route = t match {
    case SASException(error) => complete(error)
    case _ => throw t
  }

  private val register = path("user" / "register") {
    post {
      formFields('username, 'hashed_password) { (u, p) =>
        val action = (users returning users.map(_.id)) += (0, u, p)
        val future = db.run(action)
          .transform(
            userId => Map("user_id" -> userId),
            t => if (t.getMessage.contains("UNIQUE")) SASException(DuplicatedUser) else t
          )
        doComplete(future)
      }
    }
  }

  private val login = path("user" / "login") {
    post {
      formFields('username, 'hashed_password) { (u, p) =>
        val action = users
          .filter(user => user.username === u && user.hashedPassword === p)
          .map(_.id)
          .result
        val future = db.run(action)
          .map(_.headOption match {
            case Some(userId) => Map("user_id" -> userId)
            case None => throw SASException(WrongCredentials)
          })
        doComplete(future)
      }
    }
  }

  private val download = path("download" / LongNumber) { appId =>
    get {
      headerValueByType[Authorization]() { authorization =>
        headerValueByType[Date]() { date =>
          val verifyFuture = verify(date.value, authorization.value)
          val pathFuture = verifyFuture.transformWith {
            case Success(_) =>
              val action = apps.filter(_.id === appId)
                .map(_.location)
                .result
              db.run(action)
                .map(_.headOption match {
                  case Some(path) => appStorage.resolve(path)
                  case None => throw SASException(AppNotFound)
                })
            case Failure(t) => throw t
          }
          onComplete(pathFuture) {
            case Success(path) => getFromFile(path.toFile)
            case Failure(t) => handle(t)
          }
        }
      }
    }
  }

  val route: Route =
    register ~
      login ~
      download

}
