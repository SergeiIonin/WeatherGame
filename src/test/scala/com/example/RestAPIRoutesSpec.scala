package com.example

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import testutils.{PlayerHelper, WeatherHelper}
import weathergame.mongo.{FakeMongoFactoryImpl, MongoFactory, MongoService}
import weathergame.restapi.RestRoutes
import weathergame.service.{PlayerServiceActor, WeatherServiceActor}

import scala.concurrent.ExecutionContextExecutor

class RestAPIRoutesSpec extends WordSpec with BeforeAndAfterEach with Matchers with ScalaFutures
  with ScalatestRouteTest with RestRoutes with MongoService with MongoFactory {

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
  val playersInDBRequired = Set(player1ToInsert, player2ToInsert)

  private def initDBState() = {
    mongoRepository.deleteAllPlayers
    mongoRepository.insertPlayer(player1ToInsert)
    mongoRepository.insertPlayer(player2ToInsert)
  }

  private def updateDBState = {
    mongoRepository.getAllPlayersLogins().foreach(println)
    initDBState()
    /*val playersInDBActual = Set(mongoRepository.getPlayerByLogin(player1ToInsert.login),
      mongoRepository.getPlayerByLogin(player2ToInsert.login))
    if (playersInDBRequired != playersInDBActual) {
      initDBState
    }*/
  }

  override def beforeEach() = {
    // if some document is absent in mongo, one should add it. It's necessary to have 2 documents in mongo before the start
    updateDBState
  }

  lazy val routesTest = routes

  "UserRoutes" should {
    "return players by (GET /players)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/players")
     // implicit val tildeArrow: TildeArrow.InjectIntoRequestTransformer.type = TildeArrow.InjectIntoRequestTransformer

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"players":[{"description":"6 golden balls","id":"2","login":"messi"},{"description":"5 golden balls","id":"1","login":"ronaldo"}]}""".stripMargin)
      }
    }

    "return player by (GET /players/:player)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}")
      // implicit val tildeArrow: TildeArrow.InjectIntoRequestTransformer.type = TildeArrow.InjectIntoRequestTransformer

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
        status should ===(StatusCodes.Created)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"description":"no golden balls yet","id":"3","login":"neymar"}""")
      }
    }

    "return players forecast by (GET /players/:player/forecasts/:forecastId)" in {
      // note that there's no need for the host part in the uri:
      val forecast = WeatherHelper.weather1
      mongoRepository.insertForecast(player1ToInsert.login, forecast)
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/forecasts/${forecast.id}")
      // implicit val tildeArrow: TildeArrow.InjectIntoRequestTransformer.type = TildeArrow.InjectIntoRequestTransformer

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"humidity":70,"id":"1","precipitation":{"name":"rain"},"sky":{"name":"sunny"},"temperature":27,"wind":1}""".stripMargin)
      }
    }

    "return players forecasts by (GET /players/:player/forecasts)" in {
      // note that there's no need for the host part in the uri:
      val forecast1 = WeatherHelper.weather1
      val forecast3 = WeatherHelper.weather3
      mongoRepository.insertForecast(player1ToInsert.login, forecast1)
      mongoRepository.insertForecast(player1ToInsert.login, forecast3)
      val request = HttpRequest(uri = s"/players/${player1ToInsert.login}/forecasts")
      // implicit val tildeArrow: TildeArrow.InjectIntoRequestTransformer.type = TildeArrow.InjectIntoRequestTransformer

      request ~> routesTest ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[String] should === (
          """{"list":[{"humidity":70,"id":"1","precipitation":{"name":"rain"},"sky":{"name":"sunny"},"temperature":27,"wind":1},{"humidity":75,"id":"3","precipitation":{"name":"snow"},"sky":{"name":"sunny"},"temperature":2,"wind":3}]}""".stripMargin)
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
