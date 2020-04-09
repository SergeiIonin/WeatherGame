package weathergame.mongo

import java.util

import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCursor
import org.bson.Document
import org.slf4j.LoggerFactory
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.player.{Player, PlayerUtils}
import weathergame.weather.{Weather, WeatherUtils}

import scala.collection.JavaConverters._
import scala.collection.{immutable, mutable}

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

  def insertForecast(login: String, forecast: Weather) = insertWeather(login, forecast, "forecasts")

  def insertRealWeather(login: String, forecast: Weather) = insertWeather(login, forecast, "realWeathers")

  private def insertWeather(login: String, forecast: Weather, docName: String) = {
    val docForecast = Document.parse(forecast.toJson.toString)
    val update = new Document("$push", new Document(docName, docForecast))
    val filter = new Document("login", login)
    playersColl.updateOne(filter, update)
  }

  def getForecastById(login: String, forecastId: String) = {
    val query = new BasicDBObject("login", login)
    val res = playersColl.find(query)
    if (res.cursor().hasNext) {
      val cursor = res.cursor().next()
      val forecastsList = cursor.get("forecasts").asInstanceOf[util.ArrayList[Document]].asScala
      if (forecastsList.nonEmpty) {
        val forecastIds = forecastsList.map(_.get("id").toString)
        if (forecastIds contains forecastId) {
          val forecastIdToForecast = (forecastIds zip forecastsList).toMap
          val forecastDoc = forecastIdToForecast(forecastId)
          transformDocumentToWeather(forecastDoc)
        } else WeatherUtils.emptyWeather
      } else WeatherUtils.emptyWeather
    } else WeatherUtils.emptyWeather
  }

  def getAllPlayersForecasts(login: String) = {
    val query = new BasicDBObject("login", login)
    val res = playersColl.find(query)
    if (res.cursor().hasNext) {
      val cursor = res.cursor().next()
      val forecastsList = cursor.get("forecasts").asInstanceOf[util.ArrayList[Document]].asScala
      forecastsList.map(transformDocumentToWeather).toList
    } else List(WeatherUtils.emptyWeather)
  }

  private def transformDocumentToWeather(doc: Document): Weather = {
    val jsObject = transformWeatherDocToJsObject(doc)
    wthr.read(jsObject)
  }

  private def transformWeatherDocToJsObject(doc: Document) = {
    val docStringToJsStringMap = (key: String) => {
      val jsString = JsString(doc.get(key).asInstanceOf[String])
      key -> jsString
    }

    val docStringToJsNumberMap = (key: String) => {
      val jsNumber = JsNumber(doc.get(key).asInstanceOf[Int])
      key -> jsNumber
    }

    val docObjectToJsObjectMap = (key: String) => {
      val innerDoc = doc.get(key).asInstanceOf[Document]
      val jsString = JsString(innerDoc.get("name").asInstanceOf[String])
      val innerJsObj = JsObject("name" -> jsString)
      key -> innerJsObj
    }

    val weatherDocToStringJsValueMap = (doc: Document) => {
      var jsMap = immutable.Map.empty[String, JsValue]
      if (doc.containsKey("id")) jsMap+=docStringToJsStringMap("id")
      if (doc.containsKey("temperature")) jsMap+=docStringToJsNumberMap("temperature")
      if (doc.containsKey("precipitation")) jsMap+=docObjectToJsObjectMap("precipitation")
      if (doc.containsKey("sky")) jsMap+=docObjectToJsObjectMap("sky")
      if (doc.containsKey("humidity")) jsMap+=docStringToJsNumberMap("humidity")
      if (doc.containsKey("wind")) jsMap+=docStringToJsNumberMap("wind")
      if (doc.containsKey("date")) jsMap+=docStringToJsStringMap("date")
      if (doc.containsKey("location")) jsMap+=docStringToJsStringMap("location")
      jsMap
    }

    JsObject(weatherDocToStringJsValueMap(doc))
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

  private def deleteAllPlayersWeathers(login: String, docName: String) = {
    val query = new BasicDBObject("login", login)
    val player = playersColl.find(query)
    if (player.cursor().hasNext) {
      val delReq = new Document("$pull", new Document(docName, new Document()))
     playersColl.updateOne(query, delReq)
    }
  }

  private def deletePlayersWeatherById(login: String, forecastId: String, docName: String) = {
    val query = new BasicDBObject("login", login)
    val player = playersColl.find(query)
    if (player.cursor().hasNext) {
      val forecastsList = player.cursor().next().get("forecasts").
        asInstanceOf[util.ArrayList[Document]].asScala
      if (forecastsList.nonEmpty) {
        val forecastIds = forecastsList.map(_.get("id").toString)
        if (forecastIds contains forecastId) {
          val forecastIdToForecast = (forecastIds zip forecastsList).toMap
          val forecastDoc = forecastIdToForecast(forecastId)
          val delReq = new Document("$pull", new Document(docName, forecastDoc))
          playersColl.updateOne(query, delReq)
        }
      }
    }
  }

}
