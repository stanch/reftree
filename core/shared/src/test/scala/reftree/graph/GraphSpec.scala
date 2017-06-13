package reftree.graph

import org.scalatest.{Matchers, FlatSpec}
import reftree.core._
import reftree.diagram.{Animation, Diagram}
import reftree.dot.NodeId
import reftree.dot.html._
import reftree.geometry.Color
import reftree.render.RenderingOptions
import com.softwaremill.quicklens._

class GraphSpec extends FlatSpec with Matchers {
  val red = Color.RGBA(1.0, 0, 0, 1.0)
  val blue = Color.RGBA(0, 0, 1.0, 1.0)
  val weakRed = red.opacify(0.25)
  val weakBlue = blue.opacify(0.25)
  val weakViolet = Color.mix(Seq(weakRed, weakBlue))
  val weakerViolet = weakViolet.copy(a = 0.2)

  val options = RenderingOptions(palette = IndexedSeq(red, blue))

  it should "generate graphs for basic diagrams" in {
    val diagram = Diagram(List(1))
    val graph = Graphs.graph(options)(diagram)

    graph.nodes.head.label should matchPattern {
      case Table(Seq(
        RowContent(Seq(
          Cell(Plain("Cons"), Cell.Attrs(Some("n"), Some(2), _)),
          Cell(Plain("1"), Cell.Attrs(None, Some(2), _)),
          Cell(Raw("&middot;"), Cell.Attrs(Some("1"), Some(2), _))))), _) ⇒
    }

    graph.nodes.last.label should matchPattern {
      case Table(Seq(
        RowContent(Seq(
          Cell(Plain("Nil"), Cell.Attrs(Some("n"), Some(2), _))))), _) ⇒
    }

    graph.edges.head.from shouldEqual
      NodeId(graph.nodes.head.id, Some("1"), Some("s"))

    graph.edges.head.to shouldEqual
      NodeId(graph.nodes.last.id, Some("n"), Some("n"))
  }

  it should "support a mix of named and unnamed fields" in {
    case class Country(name: String, capital: String)

    implicit val derivationConfig = ToRefTree.DerivationConfig[Country]
      .rename("Country")
      .tweakField("name", _.withoutName)

    val diagram = Diagram(Country("Portugal", "Lisboa"))
    val graph = Graphs.graph(options)(diagram)

    graph.nodes.head.label should matchPattern {
      case Table(Seq(
        RowContent(Seq(
          Cell(Plain("Country"), Cell.Attrs(Some("n"), Some(2), _)),
          Cell(Raw("&middot;"), Cell.Attrs(Some("0"), Some(2), _)),
          Cell(Italic("capital"), Cell.Attrs(None, None, _)))),
        RowDivider,
        RowContent(Seq(
          Cell(Raw("&middot;"), Cell.Attrs(Some("1"), None, _))))), _) ⇒
    }
  }

  it should "honor elided references" in {
    case class Elider(elided: String)

    implicit val derivationConfig = ToRefTree.DerivationConfig[Elider]
      .rename("Elider")
      .tweakField("elided", _.withElideRefs(true))

    val string = "foo"
    val elider = Elider(string)

    val diagram1 = Diagram(elider)
    val diagram2 = Diagram(elider) + Diagram(string)

    val graph1 = Graphs.graph(options)(diagram1)
    val graph2 = Graphs.graph(options)(diagram2)

    graph1.nodes shouldEqual graph2.nodes.init
    graph1.edges shouldBe empty
    graph2.edges.head.from.id shouldEqual graph1.nodes.head.id
  }

  it should "encode anchors" in {
    val diagram = Diagram(List(1)).withAnchor("list")
    val graph = Graphs.graph(options)(diagram)

    graph.nodes.head.attrs.tooltip shouldEqual Some("anchor-list")
  }

  it should "ensure namespacing" in {
    val diagram1 = Diagram(List(1)) + Diagram(List(2))
    val diagram2 = Diagram(List(1)).withCaption("list").toNamespace("1") +
      Diagram(List(2)).withCaption("list").toNamespace("2")

    val graph1 = Graphs.graph(options)(diagram1)
    val graph2 = Graphs.graph(options)(diagram2)

    graph1.nodes.length shouldEqual 3
    graph2.nodes.length shouldEqual 6

    graph1.edges.length shouldEqual 2
    graph2.edges.length shouldEqual 4
  }

  it should "merge diagrams correctly" in {
    val list = List(1)

    val diagram = Diagram(list) + Diagram(0 :: list)
    val graph = Graphs.graph(options)(diagram)

    graph.nodes.map(_.attrs.color) shouldEqual
      Seq(Some(`red`), Some(`red`), Some(`blue`))

    graph.nodes.map(_.label).collect {
      case Table(Seq(RowContent(Seq(Cell(Plain(n), _), _*))), _) ⇒ n
    } shouldEqual Seq("Cons", "Nil", "Cons")

    graph.nodes.map(_.label).collect {
      case Table(Seq(RowContent(Seq(_, Cell(Plain(n), _), _*))), _) ⇒ n
    } shouldEqual Seq("1", "0")
  }

  it should "leave onion skin traces for animations" in {
    val animation = Animation.startWith(List(1)).iterate(_ :+ 2, _ :+ 3).build(Diagram(_))
    val graphs = Graphs.graphs(options, 2)(animation)

    val ids = graphs.map(_.nodes.map(_.id))

    ids(0).slice(0, 1) shouldEqual ids(1).slice(0, 1)
    ids(1).slice(0, 3) shouldEqual ids(2).slice(0, 3)

    val alpha = graphs.map(_.nodes.map(_.attrs.color.get.a))

    alpha(0).foreach(_ shouldEqual 1.0)

    alpha(1).slice(0, 1).foreach(_ shouldEqual 0.46 +- 0.01)
    alpha(1).drop(1).foreach(_ shouldEqual 1.0)

    alpha(2).slice(0, 1).foreach(_ shouldEqual 0.23 +- 0.01)
    alpha(2).slice(1, 3).foreach(_ shouldEqual 0.46 +- 0.01)
    alpha(2).drop(3).foreach(_ shouldEqual 1.0)
  }

  it should "mix node and cell colors when merging" in {
    val array = Array(1, 2, 3)

    def highlightCells(indexes: Set[Int]) = {
      val tree = array.refTree
        .modify(_.when[RefTree.Ref].children)
        .using(_.zipWithIndex.map { case (c, i) ⇒ c.withTreeHighlight(indexes(i)) })
        .withHighlight(true)
      Diagram.Single(tree)
    }

    val diagram = highlightCells(Set(0, 1)) + highlightCells(Set(1, 2))
    val graph = Graphs.graph(options)(diagram)

    graph.nodes.head.label should matchPattern {
      case Table(Seq(
        RowContent(Seq(
          Cell(_, _),
          Cell(_, Cell.Attrs(_, _, Some(`weakRed`))),
          Cell(_, Cell.Attrs(_, _, Some(`weakViolet`))),
          Cell(_, Cell.Attrs(_, _, Some(`weakBlue`)))))),
        Table.Attrs(_, _, _, _, Some(`weakerViolet` ), _)) ⇒
    }
  }
}
