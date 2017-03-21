import sbt.Keys._
import sbt._

object Dependencies {
  object Versions {
    val specs2 = "3.8.9"
  }

  val shapeless = Seq("com.chuusai" %% "shapeless" % "2.3.2")

  val kittens = Seq("org.typelevel" %% "kittens" % "1.0.0-M9")

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

  val withKittens = Seq(
    libraryDependencies ++= kittens ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor < 12 =>
        Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.1.0" cross CrossVersion.patch))
      case _ => Seq()
    })
  )

  val commonSettings = Seq(
    scalaVersion := "2.12.1",
    crossScalaVersions := Seq( "2.11.8", scalaVersion.value),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.bintrayRepo("scalaz", "releases")
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
  )

  val settings = commonSettings ++ Seq(
    libraryDependencies ++= test
  )

}
