package weathergame.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import weathergame.calculator.WeatherCalculatorActor
import weathergame.weather.{Weather, WeatherList, WeatherUtils}
import weathergame.gamemechanics.ResultCalculator.Result

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

  case class GetResult(forecastId: String, login: String)
  case class GetAllResults(login: String)

  sealed trait ForecastResponse
  case class ForecastCreated(forecastId: String, forecast: Weather) extends ForecastResponse
  case class ForecastFailedToBeCreated(forecastId: String) extends ForecastResponse
  case object ForecastExists extends ForecastResponse

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
    case GetForecast(forecastId, login) => {
      def notFound() = sender() ! None

      def sendEmpty() = sender() ! WeatherUtils.emptyWeather

      def getForecast(child: ActorRef) = child forward WeatherCalculatorActor.GetForecast(forecastId)

      playersForecastsMap.get(login) match {
        case forecasts@Some(ListBuffer(_*)) => {
          if (forecasts.get.contains(forecastId))
            context.child(forecastId).fold(notFound())(getForecast)
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
    case GetResult(forecastId: String, login: String) => {
      def notFound() = sender() ! None

      def sendEmpty() = sender() ! 0

      def getResult(child: ActorRef) = child forward WeatherCalculatorActor.GetResult(forecastId)

      playersForecastsMap.get(login) match {
        case forecasts@Some(ListBuffer(_*)) => {
          if (forecasts.get.contains(forecastId))
            context.child(forecastId).fold(notFound())(getResult)
          else sendEmpty()
        }
        case None => sendEmpty()
      }
    }
    case GetAllResults(login: String) => {
      {
        import akka.pattern.{ask, pipe}

        def getResults = context.children collect {
          case child if isResultApplicableToPlayer(child.path.name) =>
            self.ask(GetResult(child.path.name, login)).mapTo[Result]
        }

        def isResultApplicableToPlayer(forecastId: String) = {
          playersForecastsMap.get(login) match {
            case forecasts@Some(ListBuffer(_*)) =>
              forecasts.get.contains(forecastId)
            case None => false
          }
        }

        def convertToResults(f: Future[Iterable[Result]]) =
          f.map(l => l.toVector)

        log.info(s"before piping Result to sender")
        pipe(convertToResults(Future.sequence(getResults))) to sender()
      }
    }
  }
}
