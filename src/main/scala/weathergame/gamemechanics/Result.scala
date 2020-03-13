package weathergame.gamemechanics

import weathergame.weather.{Weather, WeatherDifference, WeatherUtils}

object ResultCalculator {

  import SubstractableInstances._
  import Substractable.SubstractableSyntax.SubstractableOps

  import DifferenceToResultMapperInstances._
  import DifferenceToResultMapper.DifferenceToResultMapperSyntax.DifferenceToResultMapperOps

  type Result = Int

    def differenceToResult(weatherL: Weather, weatherR: Weather): Result = {
      (weatherL - weatherR).toResult
    }

}
