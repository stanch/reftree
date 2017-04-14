package reftree.graph

import java.io.File

import org.scalatest.FlatSpec
import Attr.AttrSyntax
import reftree.render.{DotRenderer, RenderingOptions}

class DotSpec extends FlatSpec {
  it should "render valid dot" in {
    val graph = Graph(
      strict = true,
      directed = true,
      Node("x", "label" := <table><tr><td port="2">{ "< ht&ml >" }</td></tr></table>),
      Node(1, "label" := "quotes!\""),
      Edge(NodeId("x").withPort(2).south, NodeId(1)),
      Attrs.Node("ranksep" := 1.0)
    )

    val output = File.createTempFile("test", "dot")
    output.deleteOnExit()

    DotRenderer.render(graph, output.toPath, RenderingOptions(), "dot")
  }
}
