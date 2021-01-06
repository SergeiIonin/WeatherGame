package weathergame.weather

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.gamemechanics.ResultCalculator
import weathergame.gamemechanics.ResultCalculator.Result
import weathergame.mongo.{MongoFactory, MongoService}
import weathergame.weather.WeatherActor._

object WeatherActor {

  def props(name: String, factory: MongoFactory) = Props(new WeatherActor(name, factory))

  /**
   *
   * */
  case class AddForecast(login: String, forecast: Weather)
  case class GetForecast(login: String, `forecast-id`: String)
  case class GetRealWeather(login: String, `forecast-id`: String) // will interact with openweather API

  case class AddRealWeather(login: String, realWeather: Weather)
  case class GetResult(login: String, forecastId: String)

}

class WeatherActor(name: String, factory: MongoFactory) extends Actor with ActorLogging with MongoService with MongoFactory {

  // fixme should think about some better way to inject dependency on factory!!!
  val mongoHost = factory.mongoHost
  val mongoPort = factory.mongoPort
  val databaseName = factory.databaseName
  val playersCollection = factory.playersCollection

  var forecastsMap = Map.empty[String, Weather]
  var realWeatherMap = Map.empty[String, Weather]

  def getResult(login: String, realWeather: Weather) = {
    val forecast = mongoRepository.getForecastById(login, realWeather.id)
    ResultCalculator.differenceToResult(forecast, realWeather)
  }

  override def receive: Receive = {
    // forecast
    case AddForecast(login, forecast) => {
      log.info(s"will add forecast $forecast")
      mongoRepository.insertForecast(login, forecast)
      val weatherResultActor = context.actorOf(WeatherResultActor.props(forecast.id), s"result-${forecast.id}")
      weatherResultActor ! WeatherResultActor.GetRealWeatherByAPI(login, forecast)
    }
    case GetForecast(login, forecastId) => {
      log.info(s"sending forecast info to sender ${sender()}")
      val forecast = mongoRepository.getForecastById(login, forecastId)
      sender() ! forecast
    }
    // result
    case AddRealWeather(login, realWeather) => {
      mongoRepository.insertRealWeather(login, realWeather)
      realWeatherMap += (realWeather.id -> realWeather)
      val result = getResult(login, realWeather)
      // fixme result should be put into mongo
      log.info(s"the result of the game is $result")
      mongoRepository.insertResult(login, result)
    }
    case GetResult(login, forecastId) => {
      log.info(s"sending result to sender ${sender()}")
      // fixme result should be get from mongo
      val result = mongoRepository.getResultById(login, forecastId)
      sender() ! result
    }
    // real weather
    case GetRealWeather(login, forecastId) => {
      val realWeather = mongoRepository.getRealWeatherById(login, forecastId)
      log.info(s"sending realWeather info to sender ${sender()}, realWeather is $realWeather")
      sender() ! realWeather
    }
  }

}
