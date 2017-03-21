import sbt.Keys._
import sbt._

object Common {
  val noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)

  val settings = Seq(
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
//      "-Xlog-implicits",
      "-Xlint"
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        Seq("-Ypartial-unification")
      case _ => Seq()
    })
  )
}
