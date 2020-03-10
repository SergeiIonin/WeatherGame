package weathergame.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import weathergame.calculator.WeatherCalculatorActor
import weathergame.weather.{Weather, WeatherList, WeatherUtils}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object WeatherServiceActor {
  def props(implicit timeout: Timeout) = Props(new WeatherServiceActor)

  def name = "weatherService"

  // weather protocol
  case class CreateForecast(forecast: Weather, login: String) // just one forecast could be created
  case class GetForecast(forecastId: String, login: String)
  case class GetForecasts(login: String)

  case class GetResult(forecastId: String)

  case class GetAllResults(playerId: String)

  sealed trait ForecastResponse

  case class ForecastCreated(forecastId: String, forecast: Weather) extends ForecastResponse

  case object ForecastExists extends ForecastResponse

  case class ForecastFailedToBeCreated(forecastId: String) extends ForecastResponse

}

class WeatherServiceActor(implicit timeout: Timeout) extends Actor with ActorLogging {

  import WeatherServiceActor._
  import context._

  def createWeatherCalculatorActor(name: String) =
    context.actorOf(WeatherCalculatorActor.props(name), name)

  var playersForecastsMap = Map.empty[String, mutable.ListBuffer[String]]

  override def receive: Receive = {
    case CreateForecast(forecast, login) => {
      val forecastName = forecast.id

      def create() = {
        log.info(s"ready to create WeatherCalculatorActor $forecastName")
        val weatherCalculatorActor = createWeatherCalculatorActor(forecastName)
        if (playersForecastsMap.contains(login)) {
          playersForecastsMap.get(login).map(forecasts => forecasts.addOne(forecastName))
        }
        else playersForecastsMap += (login -> ListBuffer(forecastName))
        weatherCalculatorActor ! WeatherCalculatorActor.AddForecast(forecast)
        sender() ! ForecastCreated(forecastName, forecast)
      }

      create()
    }
    case GetForecast(id, login) => {
      def notFound() = sender() ! None

      def sendEmpty() = sender() ! WeatherUtils.emptyWeather

      def getForecast(child: ActorRef) = child forward WeatherCalculatorActor.GetForecast(id)

      playersForecastsMap.get(login) match {
        case forecasts@Some(ListBuffer(_*)) => {
          if (forecasts.get.contains(id))
            context.child(id).fold(notFound())(getForecast)
          else sendEmpty()
        }
        case None => sendEmpty()
      }
    }
    case GetForecasts(login) => {
      import akka.pattern.{ask, pipe}

      def getForecasts = context.children collect {
        case child if isForecastApplicableToPlayer(child.path.name) =>
          self.ask(GetForecast(child.path.name, login)).mapTo[Weather]
      }

      def isForecastApplicableToPlayer(forecastId: String) = {
        playersForecastsMap.get(login) match {
          case forecasts@Some(ListBuffer(_*)) =>
            forecasts.get.contains(forecastId)
          case None => false
        }
      }

      def convertToForecasts(f: Future[Iterable[Weather]]) =
        f.map(l => WeatherList(l.toVector))

      log.info(s"before piping to sender")
      pipe(convertToForecasts(Future.sequence(getForecasts))) to sender()

    }
  }
}
