package weathergame.mongo

import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterEach, FunSpecLike}
import org.slf4j.LoggerFactory
import testutils.{PlayerHelper, WeatherHelper}
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.player.PlayerUtils

import scala.collection.JavaConverters._

class MongoSpec extends FunSpecLike with BeforeAndAfterEach with WeatherServiceMarshaller with FakeMongoService {

  val log = LoggerFactory.getLogger("mongospec")

  ignore("(0) check that data from config is fetched properly") {
    val conf = ConfigFactory.load()
    assert(conf.getString("mongo.host") == "localhost")
  }

  override def afterEach(): Unit = {
    mongoRepository.deleteAllPlayers
  }

  private def insertPlayers1_2_3() = {
    val player1ToInsert = PlayerHelper.player1
    val player2ToInsert = PlayerHelper.player2
    val player3ToInsert = PlayerHelper.player3

    mongoRepository.insertPlayer(player1ToInsert)
    mongoRepository.insertPlayer(player2ToInsert)
    mongoRepository.insertPlayer(player3ToInsert)
  }

  private def insertForecasts_1_2_3_4_ToPlayer1() = {
    val player = PlayerHelper.player1
    mongoRepository.insertForecast(player.login, WeatherHelper.weather1)
    mongoRepository.insertForecast(player.login, WeatherHelper.weather2)
    mongoRepository.insertForecast(player.login, WeatherHelper.weather3)
    mongoRepository.insertForecast(player.login, WeatherHelper.weather4)
  }

  it("(1) should properly insert Player instance into mongo") {
    val player = PlayerHelper.player1
    mongoRepository.insertPlayer(player)
   // Thread.sleep(1000)
    val res = mongoRepository.getPlayerByLogin(player.login)
    log.info(s"player was fetched from mongo $res")
    assert(res.login == "ronaldo")
  }

  it("(2) should properly get player from mongo") {
    val player1ToInsert = PlayerHelper.player1
    mongoRepository.insertPlayer(player1ToInsert)
    val playerFound = mongoRepository.getPlayerByLogin("ronaldo")
    assert(playerFound.login == "ronaldo")
  }

  it("(3) should properly get all players from mongo") {
    insertPlayers1_2_3()

    val res = mongoRepository.getAllPlayers()
    val playersList = res.cursor().asScala.toList.map(doc => doc.get("login").toString)
    assert(playersList.contains("ronaldo"))
    assert(playersList.contains("messi"))
    assert(playersList.contains("neymar"))
  }

  it("(4) should properly get forecast of the player by forecastId from mongo") {
    val player = PlayerHelper.player1
    val player1Login = player.login

    mongoRepository.insertPlayer(player)
    insertForecasts_1_2_3_4_ToPlayer1()

    assert(mongoRepository.getForecastById(player1Login, "1").id == "1")
    assert(mongoRepository.getForecastById(player1Login, "2").id == "2")
    assert(mongoRepository.getForecastById(player1Login, "3").id == "3")
    assert(mongoRepository.getForecastById(player1Login, "4").id == "4")
  }

  it("(5) should properly insert Forecast without temperature document into players collection") {
    val player = PlayerHelper.player1
    val forecast = WeatherHelper.weatherWithTempNone

    mongoRepository.insertPlayer(player)
    mongoRepository.insertForecast(player.login, forecast)

    val forecastFound = mongoRepository.getForecastById(player.login, forecast.id)
    assert(forecastFound.temperature.isEmpty)
  }

  it("(6) should properly get all forecasts from the players collection") {
    val player = PlayerHelper.player1
    val player1Login = player.login

    mongoRepository.insertPlayer(player)
    insertForecasts_1_2_3_4_ToPlayer1()

    val forecasts = mongoRepository.getAllPlayersForecasts(player1Login)
    val forecastsIds = forecasts.map(_.id)
    val idsPresented = List("1", "2", "3", "4")
    idsPresented.foreach(id => assert(forecastsIds.contains(id)))
  }

  it("(7) should delete player") {
    val player = PlayerHelper.player1
    val player1Login = player.login

    mongoRepository.insertPlayer(player)

    val resBefore = mongoRepository.getPlayerByLogin(player.login)
    assert(resBefore.login == player1Login)
    mongoRepository.deletePlayerByLogin(player1Login)
    val resAfter = mongoRepository.getPlayerByLogin(player.login)
    assert(resAfter == PlayerUtils.emptyPlayer)
  }

  it("(8) should delete all player's forecasts") {
    val player = PlayerHelper.player1
    val player1Login = player.login

    mongoRepository.insertPlayer(player)
    insertForecasts_1_2_3_4_ToPlayer1()

    mongoRepository.deleteAllPlayerForecasts(player1Login)
    val forecastsAfter = mongoRepository.getAllPlayersForecasts(player1Login)
    assert(forecastsAfter.isEmpty)
  }

  it("(9) should delete players forecast by forecastId") {
    val player = PlayerHelper.player1
    val player1Login = player.login

    mongoRepository.insertPlayer(player)
    insertForecasts_1_2_3_4_ToPlayer1()

    val forecast2Id = WeatherHelper.weather2.id
    val forecast4Id = WeatherHelper.weather4.id
    mongoRepository.deletePlayerForecastById(player1Login, forecast2Id)
    mongoRepository.deletePlayerForecastById(player1Login, forecast4Id)

    val forecasts = mongoRepository.getAllPlayersForecasts(player1Login)
    val forecastsIds = forecasts.map(_.id)
    val idsPresented = List("1", "3")
    assert(forecastsIds.length == 2)
    idsPresented.foreach(id => assert(forecastsIds.contains(id)))
  }

}
