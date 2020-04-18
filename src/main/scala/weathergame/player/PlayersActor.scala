package weathergame.player

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.mongo.MongoService
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

class PlayersActor(name: String) extends Actor with ActorLogging with MongoService {

  override def receive: Receive = {
    case Add(player) => {
      log.info(s"in PlayerActor, ready to add player $player")
      mongoRepository.insertPlayer(player)
    }
    case GetPlayer(login) => {
      log.info(s"sending player info to sender ${sender()}")
      val player =  mongoRepository.getPlayerByLogin(login)
      log.info(s"player fetched is $player")
      sender() ! player
    }
  }

}
