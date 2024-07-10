addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.3")

addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.5.2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.16.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")

libraryDependencies += "net.exoego" %% "scalajs-env-jsdom-nodejs" % "2.1.0"

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
