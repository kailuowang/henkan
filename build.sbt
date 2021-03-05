import com.typesafe.sbt.SbtGit.git
import _root_.sbtcrossproject.CrossPlugin.autoImport.CrossType
import microsites._

lazy val libs = org.typelevel.libraries

val apache2 = "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")
val gh = GitHubSettings(org = "kailuowang", proj = "henkan", publishOrg = "com.kailuowang", license = apache2)

lazy val rootSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings
lazy val module = mkModuleFactory(gh.proj, mkConfig(rootSettings, commonJvmSettings, commonJsSettings))
lazy val prj = mkPrjFactory(rootSettings)

lazy val rootPrj = project
  .configure(mkRootConfig(rootSettings,rootJVM))
  .aggregate(rootJVM, rootJS, examples, docs)
  .settings(
    noPublishSettings,
    crossScalaVersions := Nil
  )


lazy val rootJVM = project
  .configure(mkRootJvmConfig(gh.proj, rootSettings, commonJvmSettings))
  .aggregate(convertM.jvm, optionalM.jvm)
  .settings(noPublishSettings,
    crossScalaVersions := Nil)


lazy val rootJS = project
  .configure(mkRootJsConfig(gh.proj, rootSettings, commonJsSettings))
  .aggregate(convertM.js, optionalM.js)
  .settings(
    noPublishSettings,
    crossScalaVersions := Nil
  )


lazy val convert    = prj(convertM)
lazy val convertM   = module("convert", CrossType.Pure)
  .settings(libs.dependency("shapeless"),
    libs.testDependencies("specs2-core", "specs2-mock"))
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "1.0.0" % Test
  )


lazy val optional    = prj(optionalM)
lazy val optionalM   = module("optional", CrossType.Pure)
  .settings(libs.dependencies("shapeless", "cats-core"),
    libs.testDependencies("specs2-core", "specs2-mock"))


lazy val examples = project
  .dependsOn(convert, optional)
  .aggregate(convert, optional)
  .settings(
    moduleName := "henkan-examples",
    commonSettings,
    noPublishSettings,
    libraryDependencies += "com.typesafe" % "config" % "1.4.1"
  )

lazy val docs = project
  .configure(mkDocConfig(gh, rootSettings, Nil, optional, convert))


lazy val devKai = Developer("Kailuo Wang", "@kailuowang", "kailuo.wang@gmail.com", new java.net.URL("http://kailuowang.com"))
lazy val commonSettings = sharedCommonSettings ++ Seq(
  parallelExecution in Test := false,
  scalaVersion := libs.vers("scalac_2.12"),
  crossScalaVersions := Seq(scalaVersion.value, libs.vers("scalac_2.13")),
  developers := List(devKai)) ++ scalacAllSettings ++ unidocCommonSettings ++ addCompilerPlugins(libs, "kind-projector") ++ Seq(
    scalacOptions ++= (if(priorTo2_13(scalaVersion.value)) Nil else
      Seq("-Ywarn-unused:-implicits", "-Xlint:-byname-implicit")),
    )


lazy val buildSettings = sharedBuildSettings(gh, libs)

lazy val commonJvmSettings = Seq()

lazy val publishSettings = sharedPublishSettings(gh) ++ credentialSettings ++ sharedReleaseProcess

lazy val scoverageSettings = sharedScoverageSettings(60)

lazy val commonJsSettings = Seq(
  scalaJSStage in Global := FastOptStage,
  // currently sbt-doctest doesn't work in JS builds
  // https://github.com/tkawachi/sbt-doctest/issues/52
  doctestGenTests := Seq.empty
)
