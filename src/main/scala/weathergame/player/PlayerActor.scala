package weathergame.player

import akka.actor.{Actor, Props}
import weathergame.calculator.WeatherCalculatorActor.Calculate
import weathergame.gamemechanics.Result
import weathergame.player.PlayerActor.{Add, GetPlayer}
import weathergame.weather.Weather

object PlayerActor {

  def props(login: String) = Props(new PlayerActor(login))

  /**
   *
   * */
  case class Add(player: Player)
  case class GetPlayer(login: String)
  case class Delete(id: String)
  case class GetRealWeather(reality: Weather) // will interact with openweather API

}

class PlayerActor(name: String) extends Actor {

  var players = Map.empty[String, Player] // player can submit several forecasts
  //var results = Map.empty[String, Result]

  override def receive: Receive = {
    case Add(player) => {
      players += (player.login -> player)
      sender() ! player
    }
    case GetPlayer(login) => {
      sender() ! Some(players.get(login))
    }
  }

}
