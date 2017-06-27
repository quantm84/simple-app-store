lazy val root = Project("simple-app-store", file("."))
  .settings(
    organization := "simple-app-store",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.2",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-core" % "10.0.7",
      "com.typesafe.slick" %% "slick" % "3.2.0",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0",
      "org.slf4j" % "slf4j-nop" % "1.7.25",
      "org.xerial" % "sqlite-jdbc" % "3.19.3"
    )
  )
