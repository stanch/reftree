val commonSettings = Seq(
  scalaVersion := "2.13.14",
  crossScalaVersions := Seq("2.12.19", "2.13.14"),
  scalacOptions ++= {
    val commonScalacOptions =
      Seq("-feature", "-deprecation", "-Xlint", "-Xfatal-warnings")

    scalaVersion.value match {
      case v if v.startsWith("2.12") =>
        commonScalacOptions ++
          Seq(
            "-Ypartial-unification",
            "-Ywarn-unused-import",
            "-language:higherKinds"
          )
      case _ =>
        commonScalacOptions :+
          "-Xlint:_,-implicit-recursion,-recurse-with-default,-unused,-byname-implicit" // scala/bug#12072
    }
  },
  Compile / doc / scalacOptions += "-no-link-warnings"
) ++ metadata

lazy val metadata = Seq(
  organization := "io.github.stanch",
  homepage := Some(url("https://stanch.github.io/reftree/")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/stanch/reftree"),
    "scm:git@github.com:stanch/reftree.git"
  )),
  developers := List(Developer(
    id="stanch",
    name="Nick Stanchenko",
    email="nick.stanch@gmail.com",
    url=url("https://github.com/stanch")
  )),
  licenses := Seq(("GPL-3.0", url("http://www.gnu.org/licenses/gpl-3.0.en.html")))
)

val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "reftree",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.12.0",
      "com.chuusai" %%% "shapeless" % "2.3.10",
      "com.lihaoyi" %%% "sourcecode" % "0.4.1",
      "com.lihaoyi" %%% "fastparse" % "3.1.0",
      "io.github.stanch" %%% "zipper" % "0.6.0",
      "com.softwaremill.quicklens" %%% "quicklens" % "1.9.7",
      "com.github.julien-truffaut" %%% "monocle-macro" % "2.1.0",
      "com.outr" %%% "scribe" % "3.13.4",
      "org.scalatest" %%% "scalatest" % "3.2.18" % Test,
      "org.scalacheck" %%% "scalacheck" % "1.17.0" % Test,
      "org.scalatestplus" %%% "scalacheck-1-17" % "3.2.18.0" % Test
    )
  )

lazy val coreJVM = core.jvm
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.3.0",
      "org.apache.xmlgraphics" % "batik-transcoder" % "1.17",
      "com.sksamuel.scrimage" % "scrimage-core" % "4.1.3",
      "de.sciss" %% "fingertree" % "1.5.5"
    )
  )

lazy val coreJS = core.js
  .enablePlugins(ScalaJSPlugin, JSDependenciesPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "org.scala-js" %%% "scalajs-java-time" % "1.0.0"
    ),
    packageJSDependencies / skip := false,
    jsDependencies ++= Seq(
      "org.webjars.npm" % "viz.js" % "1.8.2" / "1.8.2/viz.js"
    ),
    jsEnv := new net.exoego.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
  )

val demo = crossProject(JSPlatform, JVMPlatform)
  .in(file("demo"))
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val demoJVM = demo.jvm
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" % "ammonite" % "3.0.0-M1-24-26133e66" % Test cross CrossVersion.full
    )
  )

lazy val demoJS = demo.js
  .enablePlugins(ScalaJSPlugin, JSDependenciesPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    jsEnv := new net.exoego.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
  )

val site = project.in(file("site-gen"))
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .dependsOn(demoJVM)
  .settings(commonSettings)
  .settings(
    name := "reftree-site",
    moduleName := "reftree-site",
    mdocVariables := Map(
      "VERSION" -> version.value.split('+').head
    ),
    (publish / skip) := true
  )

lazy val root = project.in(file("."))
  .aggregate(coreJVM, coreJS, demoJVM, demoJS)
  .settings(commonSettings)
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

addCommandAlias("demo", "demoJVM/test:run")
addCommandAlias("site", "site/makeSite")
