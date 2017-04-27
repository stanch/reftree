val commonSettings = Seq(
  organization := "org.stanch",
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq("2.11.11", "2.12.2"),
  scalacOptions ++= Seq(
    "-feature", "-deprecation",
    "-Xlint", "-Ywarn-unused-import", "-Xfatal-warnings"
  ),
  scalacOptions in (Compile, doc) += "-no-link-warnings",
  resolvers += Resolver.bintrayRepo("stanch", "maven"),
  licenses := Seq(("GPL-3.0", url("http://www.gnu.org/licenses/gpl-3.0.en.html")))
)

val core = project.settings(commonSettings: _*).settings(
  name := "reftree",
  version := "1.0.0",
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.3.2",
    "com.lihaoyi" %% "sourcecode" % "0.1.3",
    "com.lihaoyi" %% "fastparse" % "0.4.2",
    "org.stanch" %% "zipper" % "0.5.0",
    "com.softwaremill.quicklens" %% "quicklens" % "1.4.8",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
    "org.apache.xmlgraphics" % "batik-transcoder" % "1.9",
    "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
    "com.github.julien-truffaut" %% "monocle-macro" % "1.4.0",
    "de.sciss" %% "fingertree" % "1.5.2",
    "org.scalatest" %% "scalatest" % "3.0.3" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.5" % Test
  )
)

val demo = project.settings(commonSettings: _*).dependsOn(core).settings(
  publish := {},
  publishLocal := {},
  libraryDependencies ++= Seq(
    "com.lihaoyi" % "ammonite" % "0.8.3" % Test cross CrossVersion.full
  ),
  tutSettings,
  tutTargetDirectory := baseDirectory.value / ".."
)

addCommandAlias("demo", "demo/test:run")

commonSettings
publish := {}
publishLocal := {}
