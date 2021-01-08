package restapi

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util
import akka.util.Timeout
import com.mongodb.client.result.UpdateResult
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import testutils.{PlayerHelper, WeatherHelper}
import weathergame.mongo.{FakeMongoFactoryImpl, MongoFactory, MongoService}
import weathergame.restapi.RestRoutes
import weathergame.service.{PlayerServiceActor, WeatherServiceActor}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future

class RestAPIRoutesAsyncSpec extends AsyncFunSpec with BeforeAndAfterEach with Matchers with ScalaFutures
  with ScalatestRouteTest with RestRoutes with MongoService with MongoFactory  {

  val log = LoggerFactory.getLogger("rest-api-routes-spec")

  val mongoHost = FakeMongoFactoryImpl.mongoHost
  val mongoPort = FakeMongoFactoryImpl.mongoPort
  val databaseName = FakeMongoFactoryImpl.databaseName
  val playersCollection = FakeMongoFactoryImpl.playersCollection

  implicit val requestTimeout: Timeout = util.Timeout(10, TimeUnit.SECONDS)

  def createWeatherServiceActor = system.actorOf(WeatherServiceActor.props(FakeMongoFactoryImpl), WeatherServiceActor.name)
  def createPlayerServiceActor = system.actorOf(PlayerServiceActor.props(FakeMongoFactoryImpl), PlayerServiceActor.name)

  val player1ToInsert = PlayerHelper.player1
  val player2ToInsert = PlayerHelper.player2

  private def initDBState() = {
    mongoRepository.deleteAllPlayers
    mongoRepository.insertPlayer(player1ToInsert)
    mongoRepository.insertPlayer(player2ToInsert)
  }

  private def updateDBState = {
    mongoRepository.getAllPlayersLogins().foreach(println)
    initDBState()
  }

  override def beforeEach() = {
    // if some document is absent in mongo, one should add it. It's necessary to have 2 documents
    // in mongo before the start
    updateDBState
  }

  lazy val routesTest = routes

  it("return players forecasts by (GET /players/:player/forecasts)") {
    val forecast1 = WeatherHelper.weather1
    val forecast3 = WeatherHelper.weather3

    val insert = Future.sequence(Seq(mongoRepository.insertForecast(player1ToInsert.login, forecast1),
      mongoRepository.insertForecast(player1ToInsert.login, forecast3)))
    insert.map(_ => {
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/forecasts")

      request ~> routesTest ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===(
          """{"list":[{"humidity":70,"id":"1","precipitation":{"name":"rain"},"sky":{"name":"sunny"},"temperature":27,"wind":1},{"humidity":75,"id":"3","precipitation":{"name":"snow"},"sky":{"name":"sunny"},"temperature":2,"wind":3}]}""".stripMargin)
      }
    }
    )
  }

  it("return player's realWeathers by (GET /players/:player/realWeathers)") {
    val realWeather1 = WeatherHelper.weather1
    val realWeather3 = WeatherHelper.weather3
    val insert = Future.sequence(Seq(mongoRepository.insertRealWeather(player1ToInsert.login, realWeather1),
      mongoRepository.insertRealWeather(player1ToInsert.login, realWeather3)))
    insert.map(_ => {
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/real-weathers")

      request ~> routesTest ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===(
          """{"list":[{"humidity":70,"id":"1","precipitation":{"name":"rain"},"sky":{"name":"sunny"},"temperature":27,"wind":1},{"humidity":75,"id":"3","precipitation":{"name":"snow"},"sky":{"name":"sunny"},"temperature":2,"wind":3}]}""".stripMargin)
      }
    }
    )
  }
}
