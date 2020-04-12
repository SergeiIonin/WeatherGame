package weathergame.mongo

import java.util

import com.mongodb.BasicDBObject
import org.bson.Document
import org.slf4j.LoggerFactory
import weathergame.gamemechanics.ResultCalculator.{Result, ResultUtils}
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.player.{Player, PlayerUtils}
import weathergame.weather.{Weather, WeatherUtils}

import scala.collection.JavaConverters._
import scala.collection.immutable

object MongoRepository extends WeatherServiceMarshaller {
  import spray.json._

  val playersColl = MongoFactory.collection
  val log = LoggerFactory.getLogger("mongorepo")

  def insertPlayer(player: Player) = {
    val docPlayer = Document.parse(player.toJson.toString)
    log.info(s"ready to insert player $player into mongo")
    playersColl.insertOne(docPlayer)
  }

  def getPlayerByLogin(login: String) = {
    val query = new BasicDBObject("login", login)
    val res = playersColl.find(query)
    if (res.cursor().hasNext) {
      val cursor = res.cursor().next()
      val playerLogin = cursor.get("login").toString
      val playerDescription = cursor.get("description").toString
      val playerId = cursor.get("id").toString
      Player(playerId, playerLogin, playerDescription)
    } else PlayerUtils.emptyPlayer
  }

  def getAllPlayers() = {
    playersColl.find()
  }

  def getAllPlayersLogins() = {
    log.info(s"all players fetched are = ${getAllPlayers.cursor().asScala.toVector}")
    getAllPlayers.cursor().asScala.toVector.map(_.get("login").toString)
  }

  def insertForecast(login: String, forecast: Weather) = insertWeather(login, forecast, "forecasts")

  def insertRealWeather(login: String, realWeather: Weather) = insertWeather(login, realWeather, "realWeathers")

  def getForecastById(login: String, forecastId: String) = getWeatherById(login, forecastId, "forecasts")

  def getRealWeatherById(login: String, forecastId: String) = getWeatherById(login, forecastId, "realWeathers")

  def getAllPlayersForecasts(login: String) = getAllPlayersWeathers(login, "forecasts")

  def getAllPlayersRealWeathers(login: String) = getAllPlayersWeathers(login, "realWeathers")

  def insertResult(login: String, result: Result) = {
    val docResult = Document.parse(result.toJson.toString)
    val update = new Document("$push", new Document("results", docResult))
    val filter = new Document("login", login)
    playersColl.updateOne(filter, update)
  }

  def getResultById(login: String, forecastId: String) = {
    val query = new BasicDBObject("login", login)
    val res = playersColl.find(query)
    if (res.cursor().hasNext) {
      val cursor = res.cursor().next()
      val resultsList = cursor.get("results").asInstanceOf[util.ArrayList[Document]].asScala
      if (resultsList.nonEmpty) {
        val resultIds = resultsList.map(_.get("id").toString)
        if (resultIds contains forecastId) {
          val resultIdToResult = (resultIds zip resultsList).toMap
          val ResultDoc = resultIdToResult(forecastId)
          transformDocumentToResult(ResultDoc)
        } else ResultUtils.emptyResult
      } else ResultUtils.emptyResult
    } else ResultUtils.emptyResult
  }

  def getAllResults(login: String) = {
    val query = new BasicDBObject("login", login)
    val res = playersColl.find(query)
    if (res.cursor().hasNext) {
      val cursor = res.cursor().next()
      if (cursor.containsKey("results")) {
        cursor.get("results").asInstanceOf[util.ArrayList[Document]].asScala.
          map(transformDocumentToWeather).toList
      } else List(ResultUtils.emptyResult)
    } else List(ResultUtils.emptyResult)
  }

  def deletePlayerByLogin(login: String) = {
    val query = new BasicDBObject("login", login)
    playersColl.deleteOne(query)
  }

  def deleteAllPlayersForecasts(login: String) = {
    deleteAllPlayersWeathers(login, "forecasts")
  }

  def deletePlayersForecastById(login: String, forecastId: String) = {
    deletePlayersWeatherById(login, forecastId, "forecasts")
  }

  def deleteAllPlayersRealWeathers(login: String) = {
    deleteAllPlayersWeathers(login, "realWeathers")
  }

  def deletePlayersRealWeatherById(login: String, forecastId: String) = {
    deletePlayersWeatherById(login, forecastId, "realWeathers")
  }

  def deleteAllPlayersResults(login: String) = {
    val query = new BasicDBObject("login", login)
    val player = playersColl.find(query)
    if (player.cursor().hasNext) {
      val delReq = new Document("$pull", new Document("results", new Document()))
      playersColl.updateOne(query, delReq)
    }
  }

  def deleteResultById(login: String, forecastId: String) = {
    val query = new BasicDBObject("login", login)
    val player = playersColl.find(query)
    if (player.cursor().hasNext) {
      val resultsList = player.cursor().next().get("results").
        asInstanceOf[util.ArrayList[Document]].asScala
      if (resultsList.nonEmpty) {
        val resultsIds = resultsList.map(_.get("id").toString)
        if (resultsIds contains forecastId) {
          val resultIdToResult = (resultsIds zip resultsList).toMap
          val resultDoc = resultIdToResult(forecastId)
          val delReq = new Document("$pull", new Document("results", resultDoc))
          playersColl.updateOne(query, delReq)
        }
      }
    }
  }

}
