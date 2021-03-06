lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion    = "2.6.3"
//lazy val scalaMongoVersion = "2.9.0"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.example",
      scalaVersion    := "2.13.1"
    )),
    name := "WeatherGame",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "org.typelevel"     %% "cats-core"                % "2.0.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      //"org.mongodb.scala" %% "mongo-scala-driver" % scalaMongoVersion,
      "org.mongodb" % "mongo-java-driver" % "3.12.2",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test,
/*      "com.github.fakemongo" % "fongo"                  % "2.2.0-RC2"         % Test,
      "org.objenesis"     % "objenesis"                 % "3.1"           % Test*/
    )
  )
