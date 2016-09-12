import sbt.Keys._
import sbt._

object Dependencies {
  object Versions {
    val specs2 = "3.6.6"
  }

  val shapeless = Seq("com.chuusai" %% "shapeless" % "2.3.2")

  val kittens = Seq("org.typelevel" %% "kittens" % "1.0.0-M4")

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

  val withKittens = Seq(
    libraryDependencies ++= kittens,
    addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.1.0" cross CrossVersion.full)
  )

  val commonSettings = Seq(
    scalaVersion in ThisBuild := "2.11.8",
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.bintrayRepo("scalaz", "releases")
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
  )

  val settings = commonSettings ++ Seq(
    libraryDependencies ++= test
  )

}
