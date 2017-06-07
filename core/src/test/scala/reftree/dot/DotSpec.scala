package reftree.dot

import reftree.dot.html._

import java.io.File

import org.scalatest.FlatSpec
import reftree.render.{DotRenderer, RenderingOptions}

class DotSpec extends FlatSpec {
  it should "render valid dot" in {
    val node1 = Node(
      "x",
      Table(Seq(
        RowContent(Seq(
          Cell(
            Plain("< \' ht & ml \" >"),
            Cell.Attrs(port = Some("2"))
          )
        ))
      ))
    )

    val node2 = Node("y", Plain("\'quotes!\""))

    val graph = Graph(
      strict = true,
      directed = true,
      Seq(
        node1, node2,
        Edge(NodeId(node1.id).withPort("2").south, NodeId(node2.id), "z"),
        Graph.Attrs(rankSep = Some(1.0))
      )
    )

    val output = File.createTempFile("test", "dot")
    output.deleteOnExit()

    DotRenderer.render(graph, output.toPath, RenderingOptions(), "dot")
  }
}
