val commonSettings = Seq(
  organization := "org.stanch",
  scalaVersion := "2.11.8",
  resolvers += Resolver.bintrayRepo("drdozer", "maven"),
  licenses := Seq(("GPL-3.0", url("http://www.gnu.org/licenses/gpl-3.0.en.html")))
)

val core = project.settings(commonSettings: _*).settings(
  name := "reftree",
  version := "0.7.0",
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.2.5",
    "com.lihaoyi" %% "sourcecode" % "0.1.2",
    "com.lihaoyi" %% "fastparse" % "0.4.1",
    "org.stanch" %% "zipper" % "0.2.0",
    "com.softwaremill.quicklens" %% "quicklens" % "1.4.7",
    "uk.co.turingatemyhamster" %% "gv-core" % "0.3.2",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    "batik" % "batik-transcoder" % "1.6-1",
    "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.7",
    "com.github.julien-truffaut" %% "monocle-macro" % "1.2.2",
    "de.sciss" %% "fingertree" % "1.5.2",
    "org.scalatest" %% "scalatest" % "3.0.0" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.2" % Test
  )
)

val demo = project.settings(commonSettings: _*).dependsOn(core).settings(
  publishArtifact := false,
  libraryDependencies ++= Seq(
    "com.softwaremill.quicklens" %% "quicklens" % "1.4.7",
    "com.lihaoyi" % "ammonite" % "0.7.7" % Test cross CrossVersion.full
  ),
  initialCommands in (Test, console) := {
    val predef = Seq(
      "import scala.collection.immutable._",
      "import de.sciss.fingertree._",
      "import monocle.macros.GenLens",
      "import com.softwaremill.quicklens._",
      "import zipper._",
      "import reftree._",
      "import reftree.contrib.FingerTreeInstances._",
      "import reftree.contrib.LensInstances._",
      "import reftree.contrib.ZipperInstances._",
      "import reftree.contrib.SimplifiedInstances",
      "import reftree.demo.Data._",
      "val diagram = Diagram()",
      "import diagram.{renderDefault ⇒ render}"
    ).mkString(";")
    s"""ammonite.Main("$predef").run(); System.exit(0)"""
  },
  tutSettings,
  tutTargetDirectory := baseDirectory.value / ".."
)

addCommandAlias("demo", "demo/test:console")
