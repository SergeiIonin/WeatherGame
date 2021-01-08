package weathergame.player

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.mongo.{MongoFactory, MongoService}
import weathergame.player.PlayersActor.{Add, GetPlayer}

object PlayersActor {

  def props(login: String, factory: MongoFactory) = Props(new PlayersActor(login, factory))

  /**
   *
   * */
  case class Add(player: Player)
  case class GetPlayer(login: String)
  case class Delete(id: String)

}

class PlayersActor(name: String, factory: MongoFactory) extends Actor with ActorLogging with MongoService with MongoFactory {
  import PlayersActor._

  val mongoHost = factory.mongoHost
  val mongoPort = factory.mongoPort
  val databaseName = factory.databaseName
  val playersCollection = factory.playersCollection

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
