lazy val libModuleSettings = Common.settings ++ Dependencies.settings ++ Publish.settings ++ Format.settings

lazy val henkan = project.in(file("."))
  .settings(moduleName := "henkan-all")
  .aggregate(extract, convert, k, examples, docs)
  .settings(Common.settings:_*)
  .settings(Common.noPublishing: _*)

lazy val extract = project
  .settings(moduleName := "henkan-extract")
  .settings(libModuleSettings:_*)
  .settings(Dependencies.withKittens:_*)

lazy val convert = project
  .settings(moduleName := "henkan-convert")
  .settings(libModuleSettings:_*)
  .settings(libraryDependencies ++= Dependencies.shapeless)

lazy val k = project
  .settings(moduleName := "henkan-k")
  .settings(Dependencies.withKittens:_*)
  .settings(libModuleSettings:_*)


lazy val optional = project
  .settings(moduleName := "henkan-optional")
  .settings(Dependencies.withKittens:_*)
  .settings(libModuleSettings:_*)


lazy val examples = project
  .dependsOn(extract, convert, k, optional)
  .aggregate(extract, convert, k, optional)
  .settings(moduleName := "henkan-examples")
  .settings(Common.settings:_*)
  .settings(Dependencies.settings:_*)
  .settings(Dependencies.withKittens:_*)
  .settings(Common.noPublishing: _*)
  .settings(Format.settings:_*)
  .settings(
    libraryDependencies += "com.typesafe" % "config" % "1.3.0"
  )

lazy val docs = project
  .dependsOn(extract, convert, k, optional)
  .settings(compile := (compile in Compile).dependsOn(tut).value)
  .settings(test := (test in Test).dependsOn(tut).value)
  .settings(moduleName := "henkan-docs")
  .settings(Dependencies.settings:_*)
  .settings(tutSettings)
  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .settings(tutTargetDirectory := file("."))
  .settings(Common.noPublishing: _*)
