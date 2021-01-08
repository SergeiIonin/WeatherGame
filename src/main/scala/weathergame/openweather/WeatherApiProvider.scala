package weathergame.openweather

trait WeatherApiProvider {
  def key: String
  def appUrl: String
}
