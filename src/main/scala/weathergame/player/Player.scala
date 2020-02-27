package weathergame.player

import java.util.UUID.randomUUID
import PlayerUtils._

case class Player(id: String = generateId,
                  login: String/*,
                  description: String = ""*/)

case class Players(players: Vector[Player])

object PlayerUtils {
  def generateId = randomUUID.toString
}
