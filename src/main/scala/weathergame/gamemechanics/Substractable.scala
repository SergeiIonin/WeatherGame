package weathergame.gamemechanics

import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{NoPrecipitation, NoSky, Precipitation, Sky, WeatherADT}

/**
 * This type class serves for enrichment of behaviour of Weather c.c. in the
 * way of allowing to use operations like "+" and "-" for instances of Weather,
 * taking into account possible None values of any field (e.g. Some(26) + None = None)
 *
 * The above will be advantageous when comparing forecast and real weather
 * */

trait Substractable[T] {

  def +(t1: T, t2: T): T
  def -(t1: T, t2: T): T

}

object SubstractableInstances {

  implicit val intSubstractable: Substractable[Int] = new Substractable[Int] {
    override def +(t1: Int, t2: Int) = (t1 + t2).abs
    override def -(t1: Int, t2: Int) = (t1 - t2).abs
  }

  implicit val stringSubstractable: Substractable[String] = new Substractable[String] {
    override def +(t1: String, t2: String) = ""
    override def -(t1: String, t2: String) = if (t1 == t2) t1 else ""
  }

  implicit val optionIntSubstractable: Substractable[Option[Int]] = new Substractable[Option[Int]] {
    override def +(t1: Option[Int], t2: Option[Int]) = for {
      x <- t1
      y <- t2
    } yield (x + y).abs

    override def -(t1: Option[Int], t2: Option[Int]) = for {
      x <- t1
      y <- t2
    } yield (x - y).abs
  }

  implicit val optionStringSubstractable: Substractable[Option[String]] = new Substractable[Option[String]] {
    override def +(str1: Option[String], str2: Option[String]) = str1.map(_ => "")

    override def -(str1: Option[String], str2: Option[String]) = for {
      s1 <- str1
      s2 <- str2 if s1 == s2
    } yield s1
  }

  implicit val optionPrecipitationAndSkySubstractable: Substractable[Option[WeatherADT]] = new Substractable[Option[WeatherADT]] {
    override def +(str1: Option[WeatherADT], str2: Option[WeatherADT]) = str1.map(_ => NoPrecipitation())

    override def -(str1: Option[WeatherADT], str2: Option[WeatherADT]) = for {
      s1 <- str1
      s2 <- str2 if s1 == s2
    } yield s1
  }
  implicit val optionPrecipitationSubstractable: Substractable[Option[Precipitation]] = new Substractable[Option[Precipitation]] {
    override def +(str1: Option[Precipitation], str2: Option[Precipitation]) = str1.map(_ => NoPrecipitation())

    override def -(str1: Option[Precipitation], str2: Option[Precipitation]) = for {
      s1 <- str1
      s2 <- str2 if s1 == s2
    } yield s1
  }
  implicit val optionSkySubstractable: Substractable[Option[Sky]] = new Substractable[Option[Sky]] {
    override def +(str1: Option[Sky], str2: Option[Sky]) = str1.map(_ => NoSky())

    override def -(str1: Option[Sky], str2: Option[Sky]) = for {
      s1 <- str1
      s2 <- str2 if s1 == s2
    } yield s1
  }

  implicit val weatherSubstractable: Substractable[Weather] = new Substractable[Weather] {
    override def +(w1: Weather, w2: Weather): Weather = Weather(
      Substractable.+(w1.id, w2.id),
      Substractable.+(w1.temperature, w2.temperature),
      Substractable.+(w1.precipitation, w2.precipitation),
      Substractable.+(w1.sky, w2.sky),
      Substractable.+(w1.humidity, w2.humidity),
      Substractable.+(w1.wind, w2.wind))

    override def -(w1: Weather, w2: Weather): Weather = Weather(
      Substractable.-(w1.id, w2.id),
      Substractable.-(w1.temperature, w2.temperature),
      Substractable.-(w1.precipitation, w2.precipitation),
      Substractable.-(w1.sky, w2.sky),
      Substractable.-(w1.humidity, w2.humidity),
      Substractable.-(w1.wind, w2.wind))
  }

}

object Substractable {

  def +[T](t1: T, t2: T)(implicit s: Substractable[T]) = {
    s.+(t1, t2)
  }

  def -[T](t1: T, t2: T)(implicit s: Substractable[T]) = {
    s.-(t1, t2)
  }

  object SubstractableSyntax {
    implicit class SubstractableOps[T](t: T) {
      implicit def +(w: T)(implicit s: Substractable[T]) = {
        s.+(t, w)
      }

      implicit def -(w: T)(implicit s: Substractable[T]) = {
        s.-(t, w)
      }
    }
  }

}
