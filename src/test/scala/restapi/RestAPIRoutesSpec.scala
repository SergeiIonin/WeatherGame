package restapi

import java.util.concurrent.TimeUnit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util
import akka.util.Timeout
import com.mongodb.client.result.UpdateResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncFunSpec, BeforeAndAfterEach, FunSpec, Informer, Matchers, Notifier, WordSpec, WordSpecLike}
import org.slf4j.LoggerFactory
import testutils.{PlayerHelper, WeatherHelper}
import weathergame.mongo.{FakeMongoFactoryImpl, MongoFactory, MongoService}
import weathergame.restapi.RestRoutes
import weathergame.service.{PlayerServiceActor, WeatherServiceActor}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class RestAPIRoutesSpec extends WordSpec with BeforeAndAfterEach with WordSpecLike with Matchers with ScalaFutures
  with ScalatestRouteTest with RestRoutes with MongoService with MongoFactory  {

  override protected def info: Informer = super.info

  override protected def note: Notifier = super.note

  val log = LoggerFactory.getLogger("rest-api-routes-spec")

  val mongoHost = FakeMongoFactoryImpl.mongoHost
  val mongoPort = FakeMongoFactoryImpl.mongoPort
  val databaseName = FakeMongoFactoryImpl.databaseName
  val playersCollection = FakeMongoFactoryImpl.playersCollection

  implicit val requestTimeout: Timeout = util.Timeout(10, TimeUnit.SECONDS)

  implicit val executionContext: ExecutionContextExecutor = executor  // bindingFuture.map requires an implicit ExecutionContext

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
    // if some document is absent in mongo, one should add it. It's necessary to have 2 documents in mongo before the start
    updateDBState
  }

  lazy val routesTest = routes

  // note that there's no need for the host part in the uri:
  val request = HttpRequest(uri = "/players")
  "UserRoutes" should {
    "return players by (GET /players)" in {

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"players":[{"description":"6 golden balls","id":"2","login":"messi"},{"description":"5 golden balls","id":"1","login":"ronaldo"}]}""".stripMargin)
      }
    }

    "return player by (GET /players/:player)" in {
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}")

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"description":"5 golden balls","id":"1","login":"ronaldo"}""".stripMargin)
      }
    }

    "be able to add player (POST /players/:player)" in {
      val player3ToInsert = PlayerHelper.player3
      val playerEntity0 = Marshal(player3ToInsert).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request = Post("/players/neymar").withEntity(playerEntity0)

      request ~> routesTest ~> check {
        status should === (StatusCodes.Created)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should ===("""{"description":"no golden balls yet","id":"3","login":"neymar"}""")
      }
    }

    "return players forecast by (GET /players/:player/forecasts/:forecastId)" in {
      // note that there's no need for the host part in the uri:
      val forecast = WeatherHelper.weather1
      mongoRepository.insertForecast(player1ToInsert.login, forecast)
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/forecasts/${forecast.id}")

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"humidity":70,"id":"1","precipitation":{"name":"rain"},"sky":{"name":"sunny"},"temperature":27,"wind":1}""".stripMargin)
      }
    }

    "return players realWeather by (GET /players/:player/realWeathers/:forecastId)" in {
      val forecast1 = WeatherHelper.weather1
      val realWeather1 = WeatherHelper.weather1
      mongoRepository.insertForecast(player1ToInsert.login, forecast1)
      mongoRepository.insertRealWeather(player1ToInsert.login, realWeather1)
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/real-weathers/${realWeather1.id}")

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        val x = entityAs[String]
        log.info(s"entity for request is $x")

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"humidity":70,"id":"1","precipitation":{"name":"rain"},"sky":{"name":"sunny"},"temperature":27,"wind":1}""".stripMargin)
      }
    }

    "return player's realWeathers by (GET /players/:player/realWeathers)" in {
      // note that there's no need for the host part in the uri:
      val realWeather1 = WeatherHelper.weather1
      val realWeather3 = WeatherHelper.weather3
      Future.sequence(Seq(mongoRepository.insertRealWeather(player1ToInsert.login, realWeather1),
        mongoRepository.insertRealWeather(player1ToInsert.login, realWeather3))) onComplete {
        case Success(_) => {
          val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/forecasts")

          request ~> routesTest ~> check {
            status should === (StatusCodes.OK)

            contentType should === (ContentTypes.`application/json`)

            entityAs[String] should === (
              """{"list":[{"humidity":70,"id":"1","precipitation":{"name":"rain"},"sky":{"name":"sunny"},"temperature":27,"wind":1},{"humidity":75,"id":"3","precipitation":{"name":"snow"},"sky":{"name":"sunny"},"temperature":2,"wind":3}]}""".stripMargin)
          }
        }
        case Failure(exception) => fail("test fail", exception)
      }
    }

      "return player's results by (GET /players/:player/results)" in {
        // note that there's no need for the host part in the uri:
      val result1 = WeatherHelper.result1
      val result2 = WeatherHelper.result2
      Future.sequence(Seq(mongoRepository.insertResult(player1ToInsert.login, result1),
        mongoRepository.insertResult(player1ToInsert.login, result2))) onComplete {
        case Success(_) => {
          val request1 = HttpRequest(uri = s"/players/${player1ToInsert.login}/results/${result1.id}")

          request1 ~> routesTest ~> check {
            status should === (StatusCodes.OK)

            contentType should === (ContentTypes.`application/json`)

            val x = entityAs[String]

            log.info(s"entity for request1 is $x")

            entityAs[String] should === ("""{"id":"1","res":42}""".stripMargin)
          }
          val request2 = HttpRequest(uri = s"/players/${player1ToInsert.login}/results/${result2.id}")

          request2 ~> routesTest ~> check {
            status should === (StatusCodes.OK)

            contentType should === (ContentTypes.`application/json`)

            val x = entityAs[String]

            log.info(s"entity for request2 is $x")

            entityAs[String] should === (
              """{"id":"4","res":21}}""".stripMargin)
          }
        }
        case Failure(exception) => fail("test fail", exception)
      }
    }

    "return player's result by id by (GET /players/:player/results/:forecastId)" in {
      // note that there's no need for the host part in the uri:
      val result1 = WeatherHelper.result1
      val result2 = WeatherHelper.result2
      Future.sequence(Seq(mongoRepository.insertResult(player1ToInsert.login, result1),
        mongoRepository.insertResult(player1ToInsert.login, result2))) onComplete {
        case Success(_) => {
          val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/results")

          request ~> routesTest ~> check {
            status should === (StatusCodes.OK)

            contentType should === (ContentTypes.`application/json`)

            entityAs[String] should === (
              """{"list":[{"id":"1","res":42},{"id":"2","res":21}]}""".stripMargin)
          }
        }
        case Failure(exception) => fail("test fail", exception)
      }
    }

   /* "delete player by (DELETE /players/:player)" in {
      // note that there's no need for the host part in the uri:
      val request = Delete(uri = s"/players/${player1ToInsert.login}")
      // implicit val tildeArrow: TildeArrow.InjectIntoRequestTransformer.type = TildeArrow.InjectIntoRequestTransformer

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"description":"5 golden balls","id":"1","login":"ronaldo"}""".stripMargin)
      }
    }*/
  }
}
