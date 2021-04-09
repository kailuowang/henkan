import sbt.Keys._
import sbt._

object Dependencies {
  object Versions {
    val specs2 = "4.0.0"
  }

  val shapeless = Seq("com.chuusai" %% "shapeless" % "2.3.4")
  val cats = Seq("org.typelevel" %% "cats-core" % "1.4.0")

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

  val commonSettings = Seq(
    scalaVersion := "2.12.7",
    crossScalaVersions := Seq( "2.11.12", scalaVersion.value),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases")
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
  )

  val settings = commonSettings ++ Seq(
    libraryDependencies ++= test
  )

}
