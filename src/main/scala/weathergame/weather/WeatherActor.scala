package weathergame.weather

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.gamemechanics.ResultCalculator
import weathergame.gamemechanics.ResultCalculator.Result
import weathergame.mongo.MongoService
import weathergame.weather.WeatherActor._

object WeatherActor {

  def props(name: String) = Props(new WeatherActor(name))

  /**
   *
   * */
  case class AddForecast(login: String, forecast: Weather)
  case class GetForecast(login: String, `forecast-id`: String)
  case class GetRealWeather(login: String, `forecast-id`: String) // will interact with openweather API

  case class AddRealWeather(login: String, realWeather: Weather)
  case class GetResult(login: String, forecastId: String)

}

class WeatherActor(name: String) extends Actor with ActorLogging with MongoService {

  var forecastsMap = Map.empty[String, Weather]
  var realWeatherMap = Map.empty[String, Weather]
  var resultsMap = Map.empty[String, Result]

  override def receive: Receive = {
    // forecast
    case AddForecast(login, forecast) => {
      log.info(s"will add forecast $forecast")
      mongoRepository.insertForecast(login, forecast)
      val weatherResultActor = context.actorOf(WeatherResultActor.props(forecast.id), forecast.id)
      weatherResultActor ! WeatherResultActor.GetRealWeatherAPI(login, forecast.id)
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
      // fixme differenceToResult needs only realWeather bc forecast will be fetched by id
      val res: Result = ResultCalculator.differenceToResult(forecastsMap(realWeather.id), realWeather)
      // fixme result should be put into mongo
      resultsMap += (realWeather.id -> res)
    }
    case GetResult(login, forecastId) => {
      log.info(s"sending result to sender ${sender()}")
      // fixme result should be get from mongo
      sender() ! resultsMap.getOrElse(forecastId, 0)
    }
    // real weather
    case GetRealWeather(login, forecastId) => {
      val realWeather = mongoRepository.getRealWeatherById(login, forecastId)
     sender() ! realWeather
    }
  }

}
