package weathergame.service

import akka.actor.{Actor, Props}
import akka.util.Timeout
import weathergame.calculator.WeatherCalculatorActor
import weathergame.weather.{Forecast, RealWeather}

object WeatherServiceActor {
  def props(implicit timeout: Timeout) = Props(new WeatherServiceActor)
  def name = "weatherService"

  case class CreateForecast(forecasts: List[Forecast])
  case class GetScore(forecastId: String)
  case class GetAllScores(userId: String)

  case class GetRaiting() // some improvements like pagination will be added later

  sealed trait ForecastResponse
  case class ForecastCreated(name: String, forecasts: List[Forecast]) extends ForecastResponse
  case object ForecastExists extends ForecastResponse

}

class WeatherServiceActor(implicit timeout: Timeout) extends Actor {
  import WeatherServiceActor._
  import context._

  def createWeatherCalculatorActor(name: String) =
    context.actorOf(WeatherCalculatorActor.props(name), name)

  override def receive: Receive = {
    case CreateForecast(forecasts) => {
      val name = forecasts.head.id
      def create() = {
        val weatherCalculatorActor = createWeatherCalculatorActor(name)
        weatherCalculatorActor ! WeatherCalculatorActor.Add(forecasts)
        sender() ! ForecastCreated(name, forecasts)
      }
      create()
    }
  }
}
