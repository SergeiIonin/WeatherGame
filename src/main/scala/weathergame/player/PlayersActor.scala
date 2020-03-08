package weathergame.player

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.player.PlayersActor.{Add, GetPlayer}

object PlayersActor {

  def props(login: String) = Props(new PlayersActor(login))

  /**
   *
   * */
  case class Add(player: Player)
  case class GetPlayer(login: String)
  case class Delete(id: String)

}

class PlayersActor(name: String) extends Actor with ActorLogging {

  var players = Map.empty[String, Player] // player can submit several forecasts
  var forecasts = Set.empty[String]
  //var results = Map.empty[String, Result] // todo results will have a map to link player's login and result

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
