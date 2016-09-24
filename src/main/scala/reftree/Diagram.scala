package reftree

import java.nio.file.{Paths, Path}

import uk.co.turingatemyhamster.graphvizs.dsl._

object Diagram {
  case class Options(
    verticalSpacing: Double = 0.8,
    palette: Seq[String] = Array("dodgerblue4", "forestgreen", "coral3"),
    highlightColor: String = "bisque",
    labels: Boolean = true,
    commonNodesBelongToLastTree: Boolean = false
  )

  case class AnimationOptions(
    delay: Int = 100,
    loop: Boolean = true,
    onionSkin: Int = 1,
    anchoring: Boolean = true,
    diffAccent: Boolean = false,
    verticalSpacing: Double = 0.8,
    color: String = "dodgerblue4",
    accentColor: String = "forestgreen",
    highlightColor: String = "bisque"
  ) {
    def toOptions = {
      val palette = if (onionSkin > 0) (50 to 80 by (30 / onionSkin)).take(onionSkin).map(i ⇒ s"gray$i").reverse :+ color else Seq(color)
      Options(verticalSpacing, palette, highlightColor, labels = false, commonNodesBelongToLastTree = true)
    }
  }

  private def label(tree: LabeledRefTree): Seq[Statement] = tree match {
    case LabeledRefTree(label, ref: RefTree.Ref) ⇒
      val labelNodeId = s"${ref.id}-label"
      Seq(
        labelNodeId :| AttributeAssignment("label", ID.Identifier(s"<<i>$label</i>>")),
        NodeId(labelNodeId, Some(Port(None, Some(CompassPt.S)))) -->
        NodeId(ref.id, Some(Port(Some("n"), Some(CompassPt.N))))
      )
    case _ ⇒ Seq.empty
  }

  private def node(ref: RefTree.Ref, color: String, options: Options): NodeStatement = {
    val title = s"""<td port="n">${ref.name}</td>"""
    val cells = ref.children.zipWithIndex map { case (c, i) ⇒ cell(c, i) }
    val highlight = if (ref.highlight) s"""bgcolor="${options.highlightColor}"""" else ""
    val style = s"""style="rounded" cellspacing="0" cellpadding="6" cellborder="0" columns="*" $highlight"""
    val label = s"""<<table $style><tr>${(title +: cells).mkString}</tr></table>>"""
    val labelAttribute = AttributeAssignment("label", ID.Identifier(label))
    ref.id :| ("id" := ref.id, labelAttribute, "color" := color, "fontcolor" := color)
  }

  private def cell(tree: RefTree, i: Int): String = {
    val label = tree match {
      case RefTree.Val(value: Int, Some(RefTree.Val.Bin), _) ⇒ value.toBinaryString
      case RefTree.Val(value, _, _) ⇒ value.toString.replace(" ", "_")
      case _: RefTree.Null ⇒ "&empty;"
      case _: RefTree.Elided ⇒ "&hellip;"
      case RefTree.Ref(_, id, _, _) ⇒ "&middot;"
    }
    val port = tree match {
      case RefTree.Ref(_, id, _, _) ⇒ s"""PORT="$id-$i""""
      case _ ⇒ ""
    }
    val highlight = (tree, tree.highlight) match {
      case (_, false) | (_: RefTree.Ref, _) ⇒ ""
      case _ ⇒ """bgcolor="bisque""""
    }
    s"""<td $port $highlight>$label</td>"""
  }

  private def link(id: String, tree: RefTree, i: Int, color: String): Option[EdgeStatement] = tree match {
    case RefTree.Ref(_, linkId, _, _) ⇒ Some(
      NodeId(id, Some(Port(Some(s"$linkId-$i"), Some(CompassPt.S)))) -->
        NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N)))) :|
          ("id" := s"$id-$i-$linkId", "color" := color)
    )
    case _ ⇒ None
  }

  private case class ColorlessStatement(s: Statement) {
    override def equals(other: Any) = other match {
      case ColorlessStatement(o) ⇒ (s, o) match {
        case (n1: NodeStatement, n2: NodeStatement) ⇒ n1.copy(attributes = None) == n2.copy(attributes = None)
        case (e1: EdgeStatement, e2: EdgeStatement) ⇒ e1.copy(attributes = None) == e2.copy(attributes = None)
        case (x, y) ⇒ x == y
      }
      case _ ⇒ false
    }

    override def hashCode() = s match {
      case n: NodeStatement ⇒ n.copy(attributes = None).hashCode()
      case e: EdgeStatement ⇒ e.copy(attributes = None).hashCode()
      case x ⇒ x.hashCode()
    }
  }

  private def graph(options: Options)(trees: Seq[LabeledRefTree]): Graph = {
    val graphAttrs = "graph" :| ("ranksep" := options.verticalSpacing)
    val nodeAttrs = "node" :| ("shape" := "plaintext", "fontname" := "consolas")
    val edgeAttrs = "edge" :| ("arrowsize" := "0.7")

    val labels = if (options.labels) trees.flatMap(label) else Seq.empty

    val statements: Seq[Statement] = Seq(graphAttrs, nodeAttrs, edgeAttrs) ++ labels ++ {
      def inner(tree: RefTree, color: String): Seq[Statement] = tree match {
        case r @ RefTree.Ref(_, id, children, _) ⇒
          Seq(node(r, color, options)) ++
            children.flatMap(inner(_, color)) ++
            children.zipWithIndex.flatMap { case (c, i) ⇒ link(id, c, i, color) }
        case _ ⇒
          Seq.empty
      }
      trees.map(_.tree).zipWithIndex.flatMap {
        case (tree, i) ⇒ inner(tree, options.palette(i % options.palette.length))
      }
    }

    def sharing[A](sequence: Seq[A]) =
      if (options.commonNodesBelongToLastTree) sequence.reverse.distinct.reverse else sequence.distinct
    val distinct = sharing(statements.map(ColorlessStatement)).map(_.s)
    NonStrictDigraph("g", distinct: _*)
  }

  private def accentuateDiff(graph1: Graph, graph2: Graph, accentColor: String) = {
    val previous = graph1.statements.map(ColorlessStatement).toSet
    graph2.copy(statements = graph2.statements map {
      case n: NodeStatement if !previous(ColorlessStatement(n)) ⇒
        n.copy(attributes = n.attributes.map(_ ++ Seq("color" := accentColor, "fontcolor" := accentColor)))
      case e: EdgeStatement if !previous(ColorlessStatement(e)) ⇒
        e.copy(attributes = e.attributes.map(_ ++ Seq("color" := accentColor)))
      case s ⇒ s
    })
  }

  private def graphFrames(options: AnimationOptions)(trees: Seq[LabeledRefTree]): Seq[Graph] = {
    val prefix = Seq.fill(options.onionSkin)(trees.head)
    val frames = (prefix ++ trees).sliding(options.onionSkin + 1).map(graph(options.toOptions)).toSeq
    if (!options.diffAccent) frames else {
      val accentuated = frames.sliding(2).map {
        case Seq(prev, next) ⇒ accentuateDiff(prev, next, options.accentColor)
      }.toSeq
      frames.head +: accentuated
    }
  }

  def renderDefault(trees: LabeledRefTree*): Unit =
    renderSvg(Paths.get("diagram.svg"))(trees: _*)

  def renderPng(
    output: Path,
    options: Options = Options(),
    density: Int = 100
  )(trees: LabeledRefTree*): Unit = Output.renderPng(graph(options)(trees), output, density)

  def renderSvg(
    output: Path,
    options: Options = Options()
  )(trees: LabeledRefTree*): Unit = Output.renderSvg(graph(options)(trees), output)

  def renderAnimatedGif[A: ToRefTree](
    output: Path,
    options: AnimationOptions = AnimationOptions(),
    density: Int = 100,
    silent: Boolean = true
  )(data: Seq[A]): Unit = {
    require(data.length > 1, "at least two frames should be provided")
    val trees = data.map(d ⇒ LabeledRefTree(d.toString, d.refTree))
    val frames = graphFrames(options)(trees)
    val svgs = frames.map(Output.renderSvg)
    val ids = trees.map(_.tree) collect { case RefTree.Ref(_, id, _, _) ⇒ id }
    val adjustedSvgs = SvgMagic.adjust(svgs, ids, options.anchoring)
    Output.renderAnimatedGif(adjustedSvgs, output, options, density, silent)
  }
}
