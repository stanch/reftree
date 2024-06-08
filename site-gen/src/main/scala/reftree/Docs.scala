package reftree

import reftree.build.BuildInfo.version

object Docs {
  def main(args: Array[String]): Unit = {
    val settings = mdoc.MainSettings()
      .withSiteVariables(Map("VERSION" -> version))
      .withArgs(args.toList)

    val exitCode = mdoc.Main.process(settings)
    if (exitCode != 0) sys.exit(exitCode)
  }
}
