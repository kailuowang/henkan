import sbt.Keys._
import sbt._

object Common {
  lazy val noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)

  lazy val settings = Seq(
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Xfatal-warnings",
//      "-Xlog-implicits",
      "-Ypartial-unification"
    )
  ) ++ xlint

  lazy val xlint = Seq(
    scalacOptions += {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => "-Xlint:-unused,_"
        case _ => "-Xlint"
      }
    }
  )
}
