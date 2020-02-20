package weathergame.marshalling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import weathergame.weather.{RealWeather, Weather}
import weathergame.weather.Weather._


object SprayUtils {
  trait WeatherJsonSupport extends DefaultJsonProtocol {
    implicit val realWeather = jsonFormat6(RealWeather)
    implicit val sunny = jsonFormat0(Sunny)
    implicit val cloudy = jsonFormat0(Cloudy)
    implicit val darkCloudy = jsonFormat0(DarkCloudy)
    implicit val partlyCloudy = jsonFormat0(PartlyCloudy)


   // implicit val precipitation = jsonFormat1(Sky)


/*
    implicit val photoManifestFormat = jsonFormat8(PhotoManifest)
    implicit val manifestFormat = jsonFormat1(Manifest)*/


    implicit val book = jsonFormat2(Book)
    implicit val person = jsonFormat0(Person)
    case class Book(title: String, year: Int)
    class Person() {

    }


  }
}
