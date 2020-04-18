package weathergame

import java.util

import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCollection
import org.bson.Document
import weathergame.gamemechanics.ResultCalculator.Result
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.weather.{Weather, WeatherUtils}

import scala.collection.JavaConverters._
import scala.collection.immutable

package object mongo extends WeatherServiceMarshaller {
  import spray.json._

  private[mongo] def insertWeather(playersColl: MongoCollection[Document], login: String, weather: Weather, docName: String) = {
    val docForecast = Document.parse(weather.toJson.toString)
    val update = new Document("$push", new Document(docName, docForecast))
    val filter = new Document("login", login)
    playersColl.updateOne(filter, update)
  }

  private[mongo] def getWeatherById(playersColl: MongoCollection[Document], login: String, weatherId: String,
  docName: String) = {
    val query = new BasicDBObject("login", login)
    val res = playersColl.find(query)
    if (res.cursor().hasNext) {
      val cursor = res.cursor().next()
      val weathersList = cursor.get(docName).asInstanceOf[util.ArrayList[Document]].asScala
      if (weathersList.nonEmpty) {
        val weatherIds = weathersList.map(_.get("id").toString)
        if (weatherIds contains weatherId) {
          val weatherIdToWeather = (weatherIds zip weathersList).toMap
          val weatherDoc = weatherIdToWeather(weatherId)
          transformDocumentToWeather(weatherDoc)
        } else WeatherUtils.emptyWeather
      } else WeatherUtils.emptyWeather
    } else WeatherUtils.emptyWeather
  }

  private[mongo] def getAllPlayersWeathers(playersColl: MongoCollection[Document], login: String, docName: String) = {
    val query = new BasicDBObject("login", login)
    val res = playersColl.find(query)
    if (res.cursor().hasNext) {
      val cursor = res.cursor().next()
      if (cursor.containsKey(docName)) {
        cursor.get(docName).asInstanceOf[util.ArrayList[Document]].asScala.
          map(transformDocumentToWeather).toList
      } else List(WeatherUtils.emptyWeather)
    } else List(WeatherUtils.emptyWeather)
  }

  private[mongo] def transformDocumentToWeather(doc: Document): Weather = {
    val jsObject = transformWeatherDocToJsObject(doc)
    wthr.read(jsObject)
  }

  private[mongo] def transformWeatherDocToJsObject(doc: Document) = {
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

  private[mongo] def transformDocumentToResult(doc: Document): Result = {
    val jsObject = transformResultDocToJsObject(doc)
    rslt.read(jsObject)
  }

  private[mongo] def transformResultDocToJsObject(doc: Document) = {
    val docStringToJsStringMap = (key: String) => {
      val jsString = JsString(doc.get(key).asInstanceOf[String])
      key -> jsString
    }

    val docStringToJsNumberMap = (key: String) => {
      val jsNumber = JsNumber(doc.get(key).asInstanceOf[Int])
      key -> jsNumber
    }

    val resultDocToStringJsValueMap = (doc: Document) => {
      var jsMap = immutable.Map.empty[String, JsValue]
      if (doc.containsKey("id")) jsMap+=docStringToJsStringMap("id")
      if (doc.containsKey("res")) jsMap+=docStringToJsNumberMap("res")
      jsMap
    }
    JsObject(resultDocToStringJsValueMap(doc))
  }

  private[mongo] def deleteAllPlayerWeathers(playersColl: MongoCollection[Document], login: String,
                                             docName: String) = {
    val query = new BasicDBObject("login", login)
    val player = playersColl.find(query)
    if (player.cursor().hasNext) {
      val delReq = new Document("$pull", new Document(docName, new Document()))
      playersColl.updateOne(query, delReq)
    }
  }

  private[mongo] def deletePlayerWeatherById(playersColl: MongoCollection[Document], login: String, forecastId: String,
                                             docName: String) = {
    val query = new BasicDBObject("login", login)
    val player = playersColl.find(query)
    if (player.cursor().hasNext) {
      val forecastsList = player.cursor().next().get(docName).
        asInstanceOf[util.ArrayList[Document]].asScala
      if (forecastsList.nonEmpty) {
        val forecastsIds = forecastsList.map(_.get("id").toString)
        if (forecastsIds contains forecastId) {
          val forecastIdToForecast = (forecastsIds zip forecastsList).toMap
          val forecastDoc = forecastIdToForecast(forecastId)
          val delReq = new Document("$pull", new Document(docName, forecastDoc))
          playersColl.updateOne(query, delReq)
        }
      }
    }
  }


}
