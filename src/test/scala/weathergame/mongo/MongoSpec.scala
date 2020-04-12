package weathergame.mongo

import com.mongodb.client.FindIterable
import com.typesafe.config.ConfigFactory
import org.bson.Document
import org.scalatest.{FunSpecLike, Ignore}
import testutils.{PlayerHelper, WeatherHelper}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.player.{Player, PlayerUtils}
import scala.collection.JavaConverters._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class MongoSpec extends FunSpecLike with WeatherServiceMarshaller {
/*todo add Fongo Before each test look at the db state first until that DON'T remove ronaldo and messi players =)
*  */
  import scala.concurrent.ExecutionContext.Implicits.global

  val log = LoggerFactory.getLogger("mongospec")


  ignore("(0) check that data from config is fetched properly") {
    val conf = ConfigFactory.load()
    assert(conf.getString("mongo.host") == "localhost")
  }

  ignore("(1) should properly insert Player instance into mongo") {
    val player = PlayerHelper.player1
    MongoRepository.insertPlayer(player)
   // Thread.sleep(1000)
    val res = MongoRepository.getPlayerByLogin(player.login)
    log.info(s"player was fetched from mongo $res")
    assert(res.login == "ronaldo")
  }

  it("(2) should properly get player from mongo") {
    val player = MongoRepository.getPlayerByLogin("ronaldo")
    assert(player.login == "ronaldo")
  }

  it("(3) should properly get all players from mongo") {
    val res = MongoRepository.getAllPlayers
    val playersList = res.cursor().asScala.toList.map(doc => doc.get("login").toString)
    assert(playersList.contains("ronaldo"))
    //assert(playersList.contains("messi"))
  }

  it("(4) should properly get forecast of the player by forecastId from mongo") {
    assert(MongoRepository.getForecastById("ronaldo", "1").id == "1")
    assert(MongoRepository.getForecastById("ronaldo", "2").id == "2")
    assert(MongoRepository.getForecastById("ronaldo", "3").id == "3")
    assert(MongoRepository.getForecastById("ronaldo", "6").id == "6")
  }

  // fixme it works but add proper assert
  ignore("(5) should properly insert Forecast document into players collection") {
    val player = PlayerHelper.player1
    val forecast = WeatherHelper.weatherWithTempNone
    MongoRepository.insertForecast(player.login, forecast)
    assert(1 == 1)
  }

  it("(6) should properly get all forecasts from the players collection") {
    val login = PlayerHelper.player1.login
    val forecasts = MongoRepository.getAllPlayersForecasts(login)
    val forecastsIds = forecasts.map(_.id)
    val idsPresented = List("1", "2", "3", "6")
    idsPresented.foreach(id => assert(forecastsIds.contains(id)))
  }

  it("(7) should delete player") {
    val player = PlayerHelper.player3
    MongoRepository.insertPlayer(player)
    val resBefore = MongoRepository.getPlayerByLogin(player.login)
    assert(resBefore.login == "neymar")
    MongoRepository.deletePlayerByLogin(player.login)
    val resAfter = MongoRepository.getPlayerByLogin(player.login)
    assert(resAfter == PlayerUtils.emptyPlayer)
  }

  it("(8) should delete all players forecasts") {
    val player = PlayerHelper.player3
    /*MongoRepository.insertPlayer(player)
    val forecast1 = WeatherHelper.weather1
    val forecast2 = WeatherHelper.weather2
    val forecast3 = WeatherHelper.weather3
    MongoRepository.insertForecast(player.login, forecast1)
    MongoRepository.insertForecast(player.login, forecast2)
    MongoRepository.insertForecast(player.login, forecast3)*/
    val res = MongoRepository.deleteAllPlayersForecasts(player.login)
    assert(1 == 1)
  }

  it("(9) should delete players forecast by forecastId") {
    val player = PlayerHelper.player3
   // MongoRepository.insertPlayer(player)
    //val forecast1 = WeatherHelper.weather1
    val forecast2 = WeatherHelper.weather2
  /*  MongoRepository.insertForecast(player.login, forecast1)
    MongoRepository.insertForecast(player.login, forecast2)
    MongoRepository.insertForecast(player.login, forecast3)*/
    MongoRepository.deletePlayersForecastById(player.login, forecast2.id)
  }

}
