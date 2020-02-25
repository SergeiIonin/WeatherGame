package weathergame.user

import akka.actor.{Actor, Props}
import weathergame.calculator.WeatherCalculatorActor.Calculate
import weathergame.gamemechanics.Result
import weathergame.user.UserActor.{Add, GetUser}
import weathergame.weather.Weather

object UserActor {

  def props(login: String) = Props(new UserActor(login))

  /**
   *
   * */
  case class Add(user: User)
  case class GetUser(login: String)
  case class Delete(id: String)
  case class GetRealWeather(reality: Weather) // will interact with openweather API

}

class UserActor(name: String) extends Actor {

  var users = Map.empty[String, User] // user can submit several forecasts
  //var results = Map.empty[String, Result]

  override def receive: Receive = {
    case Add(user) => {
      users += (user.login -> user)
      sender() ! user
    }
    case GetUser(login) => {
      sender() ! Some(users.get(login))
    }
  }

}
