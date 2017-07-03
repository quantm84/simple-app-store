package sas.route

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{Authorization, Date, GenericHttpCredentials}
import akka.http.scaladsl.model.{DateTime, FormData}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import sas.AppStorage
import sas.json.SASError
import sas.json.SASErrorProtocol._
import sas.table.setup
import sas.util.Crypto
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class UserSpec extends WordSpec
  with Matchers
  with BeforeAndAfterEach
  with ScalatestRouteTest {

  var dbFile: File = _
  var db: Database = _
  var route: Route = _
  val appStorage = new AppStorage(Paths.get("."))

  override protected def beforeEach(): Unit = {
    dbFile = File.createTempFile("sas", "db")
    db = Database.forURL("jdbc:sqlite:" + dbFile.getAbsolutePath)
    Await.result(db.run(setup), 5 seconds)
    route = new User(db, appStorage).route
  }

  override protected def afterEach(): Unit = {
    db.close()
    dbFile.delete()
  }

  private def login(username: String, hashedPassword: String) =
    Post("/user/login", FormData("username" -> username, "hashed_password" -> hashedPassword))

  private def register(username: String, hashedPassword: String) =
    Post("/user/register", FormData("username" -> username, "hashed_password" -> hashedPassword))

  private def download(appId: Long, timestamp: Long, authorization: String) =
    Get(s"/download/$appId")
      .withHeaders(List(
        Authorization(GenericHttpCredentials("other", authorization)),
        Date(DateTime(timestamp))
      ))

  "user/register" should {
    "only work with unique username" in {
      register("hello", "world") ~> route ~> check {
        responseAs[String] shouldEqual """{"user_id":1}"""
      }
      register("hello", "universe") ~> route ~> check {
        responseAs[SASError] shouldEqual SASError.DuplicatedUser
      }
    }
  }

  "user/login" should {
    "only work with correct credentials" in {
      register("hello", "world") ~> route ~> check {
        responseAs[String] shouldEqual """{"user_id":1}"""
      }
      login("hello", "world") ~> route ~> check {
        responseAs[String] shouldEqual """{"user_id":1}"""
      }
      login("hello", "universe") ~> route ~> check {
        responseAs[SASError] shouldEqual SASError.WrongCredentials
      }
    }
  }

  "/download/[app-id]" should {
    "detect malformed authorization string" in {
      download(0, System.currentTimeMillis(), "asdfg") ~> route ~> check {
        responseAs[SASError] shouldEqual SASError.MalformedAuthorization
      }
    }
    "detect non-existing user" in {
      download(0, System.currentTimeMillis(), "1:asdfg") ~> route ~> check {
        responseAs[SASError] shouldEqual SASError.UserNotFound
      }
    }
    "deny unverified user" in {
      register("hello", "world") ~> route ~> check {
        responseAs[String] shouldEqual """{"user_id":1}"""
      }
      download(0, System.currentTimeMillis(), "1:asdfg") ~> route ~> check {
        responseAs[SASError] shouldEqual SASError.AuthorizationFailed
      }
    }
    "verify user and detect non-existing app" in {
      register("hello", "world") ~> route ~> check {
        responseAs[String] shouldEqual """{"user_id":1}"""
      }
      val timestamp = System.currentTimeMillis()
      val date = Date(DateTime(timestamp)).value
      val authorization = Crypto.base64hmacsha1(date, "world")
      download(0, timestamp, s"1:$authorization" ) ~> route ~> check {
        responseAs[SASError] shouldEqual SASError.AppNotFound
      }
    }
  }
}
