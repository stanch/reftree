// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val commonSettings = Seq(
  scalaVersion := "2.12.10",
  crossScalaVersions := Seq("2.12.10"),
  scalacOptions ++= Seq("-feature", "-deprecation", "-Xlint", "-Xfatal-warnings", "-Ypartial-unification"),
  Compile / compile / scalacOptions  += "-Ywarn-unused-import",
  Compile / doc / scalacOptions += "-no-link-warnings"
) ++ metadata ++ publishing

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

lazy val publishing = Seq(
  useGpg := false,
  usePgpKeyHex("8ED74E385203BEB1"),
  pgpPublicRing := baseDirectory.value.getParentFile.getParentFile / ".gnupg" / "pubring.gpg",
  pgpSecretRing := baseDirectory.value.getParentFile.getParentFile / ".gnupg" / "secring.gpg",
  pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray),
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USER", ""),
    sys.env.getOrElse("SONATYPE_PASS", "")
  ),
  publishTo := Some(Opts.resolver.sonatypeStaging)
)

val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "reftree",
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "com.lihaoyi" %%% "sourcecode" % "0.2.7",
      "com.lihaoyi" %%% "fastparse" % "2.3.0",
      "io.github.stanch" %%% "zipper" % "0.5.2",
      "com.softwaremill.quicklens" %%% "quicklens" % "1.4.8",
      "com.github.julien-truffaut" %%% "monocle-macro" % "2.0.0",
      "com.outr" %%% "scribe" % "2.7.9",
      "org.scalatest" %%% "scalatest" % "3.1.4" % Test,
      "org.scalacheck" %%% "scalacheck" % "1.14.3" % Test,
      "org.scalatestplus" %%% "scalacheck-1-14" % "3.1.3.0" % Test
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.apache.xmlgraphics" % "batik-transcoder" % "1.14",
      "com.sksamuel.scrimage" % "scrimage-core" % "4.0.18",
      "de.sciss" %% "fingertree" % "1.5.5"
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

val demo = crossProject(JSPlatform, JVMPlatform)
  .in(file("demo"))
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" % "ammonite" % "2.1.0" % Test cross CrossVersion.full
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
    makeSite / mappings ++= Seq(
      file("images/teaser.gif") → "images/teaser.gif",
      file("images/queue.gif") → "images/queue.gif",
      file("images/finger.gif") → "images/finger.gif",
      file("images/tree+zipper.gif") → "images/tree+zipper.gif",
      (( demoJS / crossTarget).value / "demo-opt.js") → "js/demo.js"
    ),
    SiteScaladocPlugin.scaladocSettings( { val Jvm = config("jvm"); Jvm }, coreJVM / (Compile / packageDoc / mappings), "api/jvm"),
    SiteScaladocPlugin.scaladocSettings( { val Js =  config("js"); Js },  coreJS / (Compile/  packageDoc / mappings), "api/js"),
    tutNameFilter := """.*\.(md|json|css|html)""".r,
    tutTargetDirectory := target.value / "tut",
    GitBook / gitbookInstallDir := Some(baseDirectory.value / "node_modules" / "gitbook"),
    GitBook / sourceDirectory := tutTargetDirectory.value,
    makeSite := makeSite.dependsOn(tutQuick).dependsOn( demoJS/ ( Compile/ fullOptJS)).value,
    ghpagesNoJekyll := true,
    git.remoteRepo := "git@github.com:stanch/reftree.git"
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
