package weathergame.player

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.calculator.WeatherCalculatorActor.Calculate
import weathergame.gamemechanics.Result
import weathergame.player.PlayersActor.{Add, GetPlayer}
import weathergame.weather.Weather

object PlayersActor {

  def props(login: String) = Props(new PlayersActor(login))

  /**
   *
   * */
  case class Add(player: Player)
  case class GetPlayer(login: String)
  case class Delete(id: String)
  //case class GetRealWeather(reality: Weather) // will interact with openweather API

}

class PlayersActor(name: String) extends Actor with ActorLogging {

  var players = Map.empty[String, Player] // player can submit several forecasts
  //var results = Map.empty[String, Result]

  override def receive: Receive = {
    case Add(player) => {
      log.info(s"in PlayerActor, ready to add player $player")
      players += (player.login -> player)
      //sender() ! player
    }
    case GetPlayer(login) => {
      log.info(s"sending player info to sender ${sender()}")
      sender() ! players.getOrElse(login, PlayerUtils.emptyPlayer)
    }
  }

}