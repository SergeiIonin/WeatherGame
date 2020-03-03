package weathergame.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import weathergame.calculator.WeatherCalculatorActor
import weathergame.player.{Player, Players, PlayersActor}
import weathergame.weather.{Weather, WeatherList, WeatherUtils}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object WeatherServiceActor {
  def props(implicit timeout: Timeout) = Props(new WeatherServiceActor)

  def name = "weatherService"

  // weather protocol
  case class CreateForecast(forecast: Weather) // just one forecast could be created
  case class GetForecast(forecastId: String)

  case object GetForecasts

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

  var weatherActorsMap = Map.empty[String, ActorRef]


  def createWeatherCalculatorActor(name: String) =
    context.actorOf(WeatherCalculatorActor.props(name), name)

  var players = Set.empty[String]

  override def receive: Receive = {
    case CreateForecast(forecast) => {
      val name = forecast.id

      def create() = {
        log.info(s"ready to create WeatherCalculatorActor $name")
        val weatherCalculatorActor = createWeatherCalculatorActor(name)
        weatherActorsMap += (name -> weatherCalculatorActor)
        weatherCalculatorActor ! WeatherCalculatorActor.Calculate(forecast)
        sender() ! ForecastCreated(name, forecast)
      }

      create()
    }
    case GetForecast(id) => {
      def notFound() = sender() ! None

      def getForecast(child: ActorRef) = child forward WeatherCalculatorActor.GetForecast(id)

      weatherActorsMap.get(id).fold(notFound())(getForecast)
    }
    case GetForecasts => {
      import akka.pattern.ask
      import akka.pattern.pipe

      def getForecasts = weatherActorsMap.toList.map {
        case (name, actorRef) => actorRef.ask(GetForecast(name)).mapTo[Weather]
      }

      def convertToForecasts(f: Future[Iterable[Weather]]) =
        f.map(l => WeatherList(l.toVector))

      log.info(s"before piping to sender")
      pipe(convertToForecasts(Future.sequence(getForecasts))) to sender()

      // fixme temp uri http://localhost:5000/forecasts
      // normal uri http://localhost:5000/players/ronaldo/forecasts/for1

      /*  val r = convertToForecasts(Future.sequence(getForecasts))
        val rOnComp = r onComplete {
          case Success(forecasts: WeatherList) => log.info(s"ready to pipe forecasts list $forecasts to sender")
          case Failure(_) => WeatherList(Vector(WeatherUtils.emptyForecast))
        }
        pipe(r) to sender()*/

      // fixme STUB!!!
      /*    val forecasts = Future.apply(Iterable.single(WeatherUtils.emptyForecast))
          pipe(convertToForecasts(forecasts)) to sender()*/

      // fixme STUB!!!
      /*def notFound() = sender() ! None
      def getForecast(child: ActorRef) = child forward WeatherCalculatorActor.GetForecast("0")

      context.child("0").fold(notFound())(getForecast)*/

    }
  }
}
