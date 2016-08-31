lazy val libModuleSettings = Common.settings ++ Dependencies.settings ++ Publish.settings ++ Format.settings

lazy val henkan = project.in(file("."))
  .settings(moduleName := "henkan-all")
  .aggregate(core, convert, k, examples, docs)
  .settings(Common.settings:_*)
  .settings(Common.noPublishing: _*)

lazy val core = project
  .settings(moduleName := "henkan-extract")
  .settings(libModuleSettings:_*)

lazy val convert = project
  .settings(moduleName := "henkan-convert")
  .settings(libModuleSettings:_*)

lazy val k = project
  .settings(moduleName := "henkan-k")
  .settings(libModuleSettings:_*)


lazy val examples = project
  .dependsOn(core, convert, k)
  .aggregate(core, convert, k)
  .settings(moduleName := "henkan-examples")
  .settings(Common.settings:_*)
  .settings(Dependencies.settings:_*)
  .settings(Common.noPublishing: _*)
  .settings(Format.settings:_*)
  .settings(
    libraryDependencies += "com.typesafe" % "config" % "1.3.0"
  )

lazy val docs = project
  .dependsOn(core, convert, k)
  .settings(compile <<= (compile in Compile).dependsOn(tut))
  .settings(test <<= (test in Test).dependsOn(tut))
  .settings(moduleName := "henkan-docs")
  .settings(Dependencies.settings:_*)
  .settings(tutSettings)
  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .settings(tutTargetDirectory := file("."))
  .settings(Common.noPublishing: _*)
