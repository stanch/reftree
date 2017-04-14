val commonSettings = Seq(
  organization := "org.stanch",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
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
  version := "0.9.0",
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.3.2",
    "com.lihaoyi" %% "sourcecode" % "0.1.3",
    "com.lihaoyi" %% "fastparse" % "0.4.2",
    "org.stanch" %% "zipper" % "0.5.0",
    "com.softwaremill.quicklens" %% "quicklens" % "1.4.8",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
    "batik" % "batik-transcoder" % "1.6-1" exclude("fop", "fop"),
    "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
    "com.github.julien-truffaut" %% "monocle-macro" % "1.4.0",
    "de.sciss" %% "fingertree" % "1.5.2",
    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.5" % Test
  )
)

val demo = project.settings(commonSettings: _*).dependsOn(core).settings(
  publish := {},
  publishLocal := {},
  libraryDependencies ++= Seq(
    "com.lihaoyi" % "ammonite" % "0.8.2" % Test cross CrossVersion.full
  ),
  initialCommands in (Test, console) := {
    val predef = Seq(
      "import scala.collection.immutable._",
      "import de.sciss.fingertree._",
      "import monocle.macros.GenLens",
      "import com.softwaremill.quicklens._",
      "import zipper._",
      "import reftree.core._",
      "import reftree.diagram._",
      "import reftree.contrib.FingerTreeInstances._",
      "import reftree.contrib.LensInstances._",
      "import reftree.contrib.ZipperInstances._",
      "import reftree.contrib.SimplifiedInstances",
      "import reftree.demo.Data._",
      "import reftree.demo.Shortcuts._"
    ).mkString(";")
    s"""ammonite.Main("$predef").run(); System.exit(0)"""
  },
  tutSettings,
  tutTargetDirectory := baseDirectory.value / ".."
)

addCommandAlias("demo", "demo/test:console")

commonSettings
publish := {}
publishLocal := {}
