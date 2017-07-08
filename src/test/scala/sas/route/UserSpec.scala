package sas.route

import java.io.File
import java.nio.file.Files

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{Authorization, Date, GenericHttpCredentials}
import akka.http.scaladsl.model.{DateTime, FormData, HttpRequest}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import sas.AppStorage
import sas.json.SASError
import sas.json.SASErrorProtocol._
import sas.table._
import sas.util.Crypto
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class UserSpec extends WordSpec
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with ScalatestRouteTest {

  var dbFile: File = _
  var db: Database = _
  var route: Route = _
  val appStoragePath = Files.createTempDirectory("sas-app")
  val appStorage = new AppStorage(appStoragePath)

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

  override protected def afterAll(): Unit = {
    appStoragePath.toFile.listFiles()
      .foreach(_.delete())
    Files.delete(appStoragePath)
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

  private def download2(appId: Long, userId: Long, hashedPassword: String) = {
    val timestamp = System.currentTimeMillis()
    val date = Date(DateTime(timestamp)).value
    val authorization = Crypto.base64hmacsha1(date, "world")
    download(appId, timestamp, s"$userId:$authorization")
  }

  private def createPublisher(u: String, p: String): Long = {
    val action = (publishers returning publishers.map(_.id)) += (0, u, p)
    Await.result(db.run(action), 5 seconds)
  }

  private def createApp(bytes: Array[Byte]): Long = {
    val appPath = Files.createTempFile(appStoragePath, "sas", "app")
    Files.write(appPath, bytes)
    val location = appPath.getFileName.toString
    val publisherId = createPublisher("user", "hashed-pass")
    val action = (apps returning apps.map(_.id)) +=
      (0, "some-name", "some-desc", location, 0, 0, 0, publisherId)
    Await.result(db.run(action), 5 seconds)
  }

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
      download2(0, 1, "world") ~> route ~> check {
        responseAs[SASError] shouldEqual SASError.AppNotFound
      }
    }
    "serve app" in {
      val content = "some-content".getBytes
      val appId = createApp(content)
      register("hello", "world") ~> route ~> check {
        responseAs[String] shouldEqual """{"user_id":1}"""
      }
      download2(appId, 1, "world") ~> route ~> check {
        responseAs[Array[Byte]] shouldEqual content
      }
    }
  }
}
