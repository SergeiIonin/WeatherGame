package weathergame.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import weathergame.gamemechanics.ResultCalculator.Result
import weathergame.mongo.{MongoFactory, MongoService}
import weathergame.weather.{Weather, WeatherActor, WeatherList, WeatherUtils}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object WeatherServiceActor {
  def props(factory: MongoFactory)(implicit timeout: Timeout) = Props(new WeatherServiceActor(factory))

  def name = "weatherService"

  // weather protocol
  case class CreateForecast(login: String, forecast: Weather)
  case class GetForecast(login: String, forecastId: String)
  case class GetForecasts(login: String)
  case class GetRealWeather(login: String, forecastId: String)
  case class GetRealWeathers(login: String)

  case class GetResult(login: String, forecastId: String)
  case class GetResults(login: String)

  sealed trait ForecastResponse
  case class ForecastCreated(forecastId: String, forecast: Weather) extends ForecastResponse
  case class ForecastFailedToBeCreated(forecastId: String) extends ForecastResponse
  case object ForecastExists extends ForecastResponse

}

class WeatherServiceActor(factory: MongoFactory)(implicit timeout: Timeout) extends Actor with ActorLogging
  with MongoService with MongoFactory {

  import WeatherServiceActor._
  import context._

  val mongoHost = factory.mongoHost
  val mongoPort = factory.mongoPort
  val databaseName = factory.databaseName
  val playersCollection = factory.playersCollection

  def getWeatherActor(name: String) =
    context.child(name).getOrElse(context.actorOf(WeatherActor.props(name, factory), name))

  def getAllWeatherActors(login: String) =
    playersForecastsMap.getOrElse(login, ListBuffer.empty).map(getWeatherActor).toList

  def convertToWeathers(f: Future[Iterable[Weather]]) = f.map(l => WeatherList(l.toVector))

  def weatherNotFound() = sender() ! WeatherUtils.emptyWeather

  var playersForecastsMap = Map.empty[String, mutable.ListBuffer[String]]

  override def preStart() = {
    super.preStart()
    log.info("in preStart() of WSA")
    val playersForecastsTuples =
      for {
      login <- mongoRepository.getAllPlayersLogins
      _ = log.info(s"login is $login")
      forecastsIds = ListBuffer(mongoRepository.getAllPlayersForecasts(login).map(_.id)).flatten
      _ = log.info(s"forecastsIds are $forecastsIds")
      } yield (login, forecastsIds)
    playersForecastsMap = playersForecastsTuples.toMap
    log.info(s"playersForecastsMap was created = ${playersForecastsMap.mkString("\n")}")
  }

  override def receive: Receive = {
    case CreateForecast(login, forecast) => {
      val forecastId = forecast.id

      def create() = {
        log.info(s"ready to create WeatherCalculatorActor $forecastId")
        val weatherCalculatorActor = getWeatherActor(forecastId)
        if (playersForecastsMap.contains(login)) {
          playersForecastsMap.get(login).map(forecasts => forecasts.addOne(forecastId))
        }
        else playersForecastsMap += (login -> ListBuffer(forecastId))
        weatherCalculatorActor ! WeatherActor.AddForecast(login, forecast)
        sender() ! ForecastCreated(forecastId, forecast)
      }

      create()
    }
    case GetForecast(login, forecastId) => {

      log.info(s"in WSA, GetForecast($login, $forecastId)")
      if (playersForecastsMap.getOrElse(login, ListBuffer.empty).contains(forecastId)) {
        getForecast(getWeatherActor(forecastId))
      } else weatherNotFound()

      def getForecast(child: ActorRef) = child forward WeatherActor.GetForecast(login, forecastId)
    }
    case GetForecasts(login) => {
      import akka.pattern.{ask, pipe}

      val allWeatherActors = getAllWeatherActors(login)
      pipe(convertToForecasts(Future.sequence(getForecasts(allWeatherActors)))) to sender()

      def getForecasts(actorRefList: Seq[ActorRef]) =
        actorRefList.map(actorRef => self.ask(GetForecast(login, actorRef.path.name)).mapTo[Weather])
      def convertToForecasts(f: Future[Iterable[Weather]]) = f.map(l => WeatherList(l.toVector))
    }
    case GetRealWeather(login, forecastId) => {

      if (playersForecastsMap.getOrElse(login, ListBuffer.empty).contains(forecastId)) {
        getRealWeather(getWeatherActor(forecastId))
      } else weatherNotFound()

      def getRealWeather(child: ActorRef) = child forward WeatherActor.GetRealWeather(login, forecastId)

    }
    case GetRealWeathers(login) => {
      import akka.pattern.{ask, pipe}

      def getRealWeathers(actorRefList: Seq[ActorRef]) =
      actorRefList.map(actorRef => self.ask(GetForecast(actorRef.path.name, login)).mapTo[Weather])

      val allWeatherActors = getAllWeatherActors(login)
      pipe(convertToWeathers(Future.sequence(getRealWeathers(allWeatherActors)))) to sender()

    }
    case GetResult(login, forecastId) => {
      def notFound() = sender() ! None

      def sendEmpty() = sender() ! 0

      def getResult(child: ActorRef) = child forward WeatherActor.GetResult(login, forecastId)

      playersForecastsMap.get(login) match {
        case forecasts@Some(ListBuffer(_*)) => {
          if (forecasts.get.contains(forecastId))
            context.child(forecastId).fold(notFound())(getResult)
          else sendEmpty()
        }
        case None => sendEmpty()
      }
    }
    case GetResults(login) => {
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
