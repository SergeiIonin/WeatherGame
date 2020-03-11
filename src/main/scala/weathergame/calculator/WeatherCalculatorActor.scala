package weathergame.calculator

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.calculator.WeatherCalculatorActor.{AddForecast, AddResult, GetForecast, GetRealWeather}
import weathergame.gamemechanics.ResultCalculator
import weathergame.gamemechanics.ResultCalculator.Result
import weathergame.weather.{Weather, WeatherUtils}

object WeatherCalculatorActor {

  def props(name: String) = Props(new WeatherCalculatorActor(name))

  /**
   *
   * */
  case class AddForecast(forecast: Weather)
  case class GetForecast(`forecast-id`: String)
  case class GetRealWeather(`forecast-id`: String) // will interact with openweather API
  case class AddResult(realWeather: Weather, `forecast-id`: String)

}

class WeatherCalculatorActor(name: String) extends Actor with ActorLogging {

  var forecastsMap = Map.empty[String, Weather]
  var realWeatherMap = Map.empty[String, Weather]
  var resultsMap = Map.empty[String, Result]

  override def receive: Receive = {
    case AddForecast(forecast) => {
      log.info(s"will add forecast $forecast")
      forecastsMap += (forecast.id -> forecast)
      val weatherResultActor = context.actorOf(WeatherResultActor.props(forecast.id), forecast.id)
      weatherResultActor ! WeatherResultActor.GetRealWeatherAPI(forecast)
    }
    case GetForecast(forecastId) => {
      log.info(s"sending forecast info to sender ${sender()}")
      sender() ! forecastsMap.getOrElse(forecastId, WeatherUtils.emptyWeather)
    }

    case AddResult(realWeather, forecastId) => {
      realWeatherMap += (forecastId -> realWeather)
      // fixme calculate result then!
      val res: Result = ResultCalculator.compare(forecastsMap(forecastId), realWeather)
      resultsMap += (forecastId -> res)
    }
    case GetRealWeather(forecastId) => {
     sender() ! realWeatherMap.getOrElse(forecastId, WeatherUtils.emptyWeather)
    }
  }

}
