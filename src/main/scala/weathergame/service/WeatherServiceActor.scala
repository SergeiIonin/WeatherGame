package weathergame.service

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import weathergame.calculator.WeatherCalculatorActor
import weathergame.user.UserActor
import weathergame.weather.{User, Users, Weather}

import scala.concurrent.Future

object WeatherServiceActor {
  def props(implicit timeout: Timeout) = Props(new WeatherServiceActor)
  def name = "weatherService"

  // user protocol
  case class CreateUser(user: User)
  case class GetUser(login: String)
  case object GetUsers
  case class GetRaiting() // some improvements like pagination will be added later

  sealed trait UserResponse
  case class UserCreated(login: String) extends UserResponse
  case object UserExists extends UserResponse

  // weather protocol
  case class CreateForecast(forecast: Weather) // just one forecast could be created
  case class GetResult(forecastId: String)
  case class GetAllResults(userId: String)

  sealed trait ForecastResponse
  case class ForecastCreated(name: String, forecast: Weather) extends ForecastResponse
  case object ForecastExists extends ForecastResponse

}

class WeatherServiceActor(implicit timeout: Timeout) extends Actor {
  import WeatherServiceActor._
  import context._

  def createWeatherCalculatorActor(name: String) =
    context.actorOf(WeatherCalculatorActor.props(name), name)

  def createUserActor(login: String) =
    context.actorOf(UserActor.props(login), login)

  var users = Set.empty[String]

  override def receive: Receive = {
    case CreateUser(user) => {
      val newLogin = user.login

      def create() = {
        if (!users.contains(newLogin)) {
          users += newLogin
          val userActor = createUserActor(newLogin)
          userActor ! UserActor.Add(user)
          sender() ! UserCreated(newLogin)
        } else
          sender() ! UserExists
      }

      create()
    }
    case GetUser(login) => {
      def notFound() = sender() ! None
      def getUser(child: ActorRef) = child forward UserActor.GetUser
      context.child(login).fold(notFound())(getUser)
    }
    case GetUsers => {
      import akka.pattern.ask
      import akka.pattern.pipe

      def getUsers = context.children.map { child =>
        self.ask(GetUser(child.path.name)).mapTo[Option[User]]
      }
      def convertToUsers(f: Future[Iterable[Option[User]]]) =
        f.map(_.flatten).map(l => Users(l.toVector))

      pipe(convertToUsers(Future.sequence(getUsers))) to sender()
    }

    case CreateForecast(forecast) => {
      val name = forecast.id
      def create() = {
        val weatherCalculatorActor = createWeatherCalculatorActor(name)
        weatherCalculatorActor ! WeatherCalculatorActor.Calculate(forecast)
        sender() ! ForecastCreated(name, forecast)
      }
      create()
    }
  }
}
