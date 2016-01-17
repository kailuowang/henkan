import sbt.Keys._
import sbt._

object Dependencies {
  object Versions {
    val specs2 = "3.6.6"
  }

  val shapeless = Seq("com.chuusai" %% "shapeless" % "2.2.5")

  val cat = Seq("org.spire-math" %% "cats" % "0.3.0")

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

  val commonSettings = Seq(
    scalaVersion in ThisBuild := "2.11.7",
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
  )

  val testSettings = commonSettings ++ Seq(
    libraryDependencies ++= test
  )

  val settings = commonSettings ++ Seq(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.bintrayRepo("scalaz", "releases")
    )
  )

}
