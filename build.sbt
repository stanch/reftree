val commonSettings = Seq(
  organization := "org.stanch",
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq("2.11.11", "2.12.2"),
  scalacOptions ++= Seq(
    "-feature", "-deprecation",
    "-Xlint", "-Xfatal-warnings"
  ),
  scalacOptions in (Compile, compile) += "-Ywarn-unused-import",
  scalacOptions in (Compile, doc) += "-no-link-warnings",
  resolvers += Resolver.bintrayRepo("stanch", "maven"),
  licenses := Seq(("GPL-3.0", url("http://www.gnu.org/licenses/gpl-3.0.en.html")))
)

val core = crossProject.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "reftree",
    version := "1.1.0",
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.2",
      "com.lihaoyi" %%% "sourcecode" % "0.1.3",
      "com.lihaoyi" %%% "fastparse" % "0.4.2",
      "org.stanch" %%% "zipper" % "0.5.1",
      "com.softwaremill.quicklens" %%% "quicklens" % "1.4.8",
      "com.github.julien-truffaut" %%% "monocle-macro" % "1.4.0",
      "com.outr" %%% "scribe" % "1.4.2",
      "org.scalatest" %%% "scalatest" % "3.0.3" % Test,
      "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "org.apache.xmlgraphics" % "batik-transcoder" % "1.9",
      "com.sksamuel.scrimage" %% "scrimage-core" % "3.0.0-alpha3",
      "de.sciss" %% "fingertree" % "1.5.2"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    ),
    jsDependencies ++= Seq(
      "org.webjars.npm" % "viz.js" % "1.7.0" / "1.7.0/viz.js"
    )
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

val demo = crossProject.in(file("demo"))
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    publish := {},
    publishLocal := {}
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" % "ammonite" % "0.8.3" % Test cross CrossVersion.full
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true
  )

lazy val demoJVM = demo.jvm
lazy val demoJS = demo.js

val site = project.in(file("site"))
  .enablePlugins(TutPlugin, GitBookPlugin, GhpagesPlugin)
  .dependsOn(demoJVM)
  .settings(commonSettings)
  .settings(
    mappings in makeSite ++= Seq(
      file("images/teaser.gif") → "images/teaser.gif",
      file("images/queue.gif") → "images/queue.gif",
      file("images/finger.gif") → "images/finger.gif",
      file("images/tree+zipper.gif") → "images/tree+zipper.gif",
      ((crossTarget in demoJS).value / "demo-opt.js") → "js/demo.js"
    ),
    SiteScaladocPlugin.scaladocSettings(config("jvm"), mappings in (Compile, packageDoc) in coreJVM, "api/jvm"),
    SiteScaladocPlugin.scaladocSettings(config("js"), mappings in (Compile, packageDoc) in coreJS, "api/js"),
    tutNameFilter := """.*\.(md|json|css|html)""".r,
    tutTargetDirectory := target.value / "tut",
    gitbookInstallDir in GitBook := Some(baseDirectory.value / "node_modules" / "gitbook"),
    sourceDirectory in GitBook := tutTargetDirectory.value,
    makeSite := makeSite.dependsOn(tutQuick).dependsOn(fullOptJS in Compile in demoJS).value,
    ghpagesNoJekyll := true,
    git.remoteRepo := "git@github.com:stanch/reftree.git"
  )

lazy val root = project.in(file("."))
  .aggregate(coreJVM, coreJS, demoJVM, demoJS)
  .settings(commonSettings)
  .settings(
    publish := {},
    publishLocal := {}
  )

addCommandAlias("demo", "demoJVM/test:run")
addCommandAlias("site", "site/makeSite")
