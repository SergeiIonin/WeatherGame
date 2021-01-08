package weathergame.gamemechanics

import weathergame.weather.{Weather, WeatherDifference, WeatherUtils}

object ResultCalculator {

  import SubstractableInstances._
  import Substractable.SubstractableSyntax.SubstractableOps

  import DifferenceToResultMapperInstances._
  import DifferenceToResultMapper.DifferenceToResultMapperSyntax.DifferenceToResultMapperOps

  case class Result(id: String, res: Option[Int] = None)

    def differenceToResult(weatherL: Weather, weatherR: Weather): Result = {
      (weatherL - weatherR).toResult
    }

  case class ResultList(list: Vector[Result])

  object ResultUtils {
    def emptyResult = Result("", Some(0))
  }

}
