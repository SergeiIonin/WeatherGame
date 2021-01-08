package weathergame.utils

import scala.collection.mutable.ListBuffer

object WeatherGameUtils {

  def listToListBuffer[T](list: List[T]) = {
    val listBuffer: ListBuffer[Any] = ListBuffer(list).flatten
  }

}
