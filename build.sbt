organization := "org.stanch"

name := "reftree"

version := "0.3.0"

licenses := Seq(("GPL-3.0", url("http://www.gnu.org/licenses/gpl-3.0.en.html")))

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.bintrayRepo("drdozer", "maven")
)

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.2.5",
  "com.lihaoyi" %% "sourcecode" % "0.1.1",
  "uk.co.turingatemyhamster" %% "gv-core" % "0.3.2",
  "com.softwaremill.quicklens" %% "quicklens" % "1.4.6" % Provided,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "com.lihaoyi" % "ammonite-repl" % "0.5.7" % Test cross CrossVersion.full
)

val predef = Seq(
  "import reftree._",
  "import scala.collection.immutable._",
  "val defaultDiagram = Diagram(); import defaultDiagram.show"
).mkString(";")

initialCommands in (Test, console) := s"""ammonite.repl.Main.run("$predef"); System.exit(0)"""

addCommandAlias("amm", "test:console")

tutSettings

tutTargetDirectory := baseDirectory.value
