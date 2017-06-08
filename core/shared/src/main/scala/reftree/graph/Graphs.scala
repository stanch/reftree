package reftree.graph

import reftree.geometry.Color
import reftree.render.RenderingOptions
import reftree.diagram.{Animation, Diagram}
import reftree.core.RefTree
import reftree.dot._

object Graphs {
  private def graphAttributes(options: RenderingOptions): Seq[GraphStatement] = Seq(
    Graph.Attrs(
      rankSep = Some(options.verticalSpacing)
    ),
    Node.Attrs(
      shape = Some("plaintext"),
      fontName = Some(options.font),
      fontColor = Some(Color.fromRgbaString("#000000"))
    ),
    Edge.Attrs(
      arrowSize = Some(0.7),
      color = Some(Color.fromRgbaString("#000000"))
    )
  )

  private def graphStatements(diagram: Diagram, options: RenderingOptions): Seq[GraphStatement] = {
    def inner(
      tree: RefTree,
      color: Color,
      anchorId: Option[String],
      namespace: Seq[String],
      depth: Int
    ): Seq[GraphStatement] = tree match {
      case r @ RefTree.Ref(_, id, children, _) ⇒
        Seq(Primitives.node(r, color, anchorId, namespace)) ++
          children.filterNot(_.elideRefs).flatMap(c ⇒ inner(c.value, color, None, namespace, depth + 1)) ++
          children.zipWithIndex.flatMap { case (c, i) ⇒ Primitives.edge(id, c.value, i, color, namespace) }
      case _ if depth == 0 ⇒
        Seq(Primitives.node(tree, color, anchorId, namespace))
      case _ ⇒
        Seq.empty
    }

    val spareColorIndices = Iterator.from(0).filterNot(diagram.fragments.flatMap(_.colorIndex).toSet)
    val colorIndices = diagram.fragments.map(_.colorIndex.getOrElse(spareColorIndices.next()))

    (diagram.fragments zip colorIndices) flatMap {
      case (fragment, i) ⇒
        val color = options.palette(i % options.palette.length)
        fragment.caption.toSeq.flatMap(Primitives.caption(_, fragment.tree, color, fragment.namespace)) ++
          inner(fragment.tree, color, fragment.anchorId, fragment.namespace, depth = 0)
    }
  }

  def graph(options: RenderingOptions)(diagram: Diagram): Graph = {
    val statements = graphAttributes(options) ++ Merging.mergeLayer(graphStatements(diagram, options))
    Graph(strict = false, directed = true, statements)
  }

  def graphs(options: RenderingOptions, onionSkinLayers: Int)(animation: Animation): Seq[Graph] = {
    val prefix = Seq.fill(onionSkinLayers)(animation.diagrams.head)
    (prefix ++ animation.diagrams).sliding(onionSkinLayers + 1).toVector map { diagrams ⇒
      val onionSkin = diagrams.init.zipWithIndex.map {
        case (diagram, i) ⇒
          val factor = (i + 1.0) / diagrams.length * 0.7
          val onionPalette = options.mapPalette(_.saturate(0.7).opacify(factor))
          graphStatements(diagram.withoutAnchors.withoutCaptions, onionPalette)
      }
      val statementLayers = onionSkin :+ graphStatements(diagrams.last, options)
      val statements = graphAttributes(options) ++ Merging.mergeLayers(statementLayers)
      Graph(strict = false, directed = true, statements)
    }
  }
}
