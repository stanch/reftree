organization := "org.stanch"

name := "reftree"

version := "0.2.0"

licenses := Seq(("GPL-3.0", url("http://www.gnu.org/licenses/gpl-3.0.en.html")))

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.bintrayRepo("drdozer", "maven")
)

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.2.5",
  "com.lihaoyi" %% "sourcecode" % "0.1.1",
  "com.github.jlmauduy" %% "ascii-graphs" % "0.0.7",
  "uk.co.turingatemyhamster" %% "gv-core" % "0.3.2",
  "com.softwaremill.quicklens" %% "quicklens" % "1.4.6" % Provided,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "com.lihaoyi" % "ammonite-repl" % "0.5.4" % Test cross CrossVersion.full
)

initialCommands in (Test, console) := """ammonite.repl.Main.run(""); System.exit(0)"""

addCommandAlias("amm", "test:console")

tutSettings

tutTargetDirectory := baseDirectory.value
