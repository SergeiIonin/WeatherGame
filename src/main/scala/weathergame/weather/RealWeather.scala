package weathergame.weather

case class RealWeather extends Weather

object RealWeather {
  def apply(): RealWeather = new RealWeather()
}
