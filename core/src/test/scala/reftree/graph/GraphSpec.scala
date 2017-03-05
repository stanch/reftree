package reftree.graph

import java.io.File

import org.scalatest.FlatSpec
import reftree.core._
import reftree.diagram.Diagram
import reftree.render.{DotRenderer, RenderingOptions}

class GraphSpec extends FlatSpec {
  it should "perform HTML escaping" in {
    val tree = List("<angular>", "&", "\"quotes\"").refTree
      .asInstanceOf[RefTree.Ref].rename("\"< & >\"")
    val diagram = Diagram.Single(tree, Some("\"< & >\""))

    val options = RenderingOptions()
    val graph = Graphs.graph(options)(diagram)

    val output = File.createTempFile("test", "dot")
    output.deleteOnExit()

    DotRenderer.render(graph, output.toPath, options, "dot")
  }
}
