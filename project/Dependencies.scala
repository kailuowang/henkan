import sbt.Keys._
import sbt._

object Dependencies {
  object Versions {
    val specs2 = "3.8.9"
  }

  val shapeless = Seq("com.chuusai" %% "shapeless" % "2.3.2")
  val cats = Seq("org.typelevel" %% "cats-core" % "1.0.0-RC2")

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

  val commonSettings = Seq(
    scalaVersion := "2.12.3",
    crossScalaVersions := Seq( "2.11.11", scalaVersion.value),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases")
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
  )

  val settings = commonSettings ++ Seq(
    libraryDependencies ++= test
  )

}
