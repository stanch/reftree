package reftree.svg

import reftree.geometry._

object SvgGraphAlignment {
  private val graph = SvgLenses.collectFirst(sel"g.graph")
  private val nodes = SvgLenses.collectById(sel"g.node")

  private val nodeAnchor = SvgLenses.collectFirst(sel"a") composeLens
    SvgLenses.prefixedAttr("http://www.w3.org/1999/xlink", "title")

  private val nodePosition = SvgLenses.collectFirst(sel"text") composeOptional
    SvgLenses.textPosition

  private def groupByAnchor(nodes: Map[String, xml.Node]) =
    nodes.values.groupBy(nodeAnchor.getOption(_).flatten) flatMap {
      case (Some(anchor), group) ⇒ Some((anchor, group.head))
      case _ ⇒ None
    }

  private def mapDelta(prev: Map[String, xml.Node], next: Map[String, xml.Node]) = {
    val common = prev.keySet & next.keySet
    if (common.isEmpty) None else {
      val deltas = common map { id ⇒
        nodePosition.getOption(prev(id)).get - nodePosition.getOption(next(id)).get
      }
      Some(Point.mean(deltas.toSeq))
    }
  }

  private def seqDelta(prev: Seq[xml.Node], next: Seq[xml.Node]) = {
    Point.mean(prev.map(nodePosition.getOption(_).get)) -
      Point.mean(next.map(nodePosition.getOption(_).get))
  }

  /** Align two adjacent animation frames */
  def align(prev: xml.Node, next: xml.Node) = {
    val prevNodes = nodes.get(prev)
    val nextNodes = nodes.get(next)
    val prevAnchors = groupByAnchor(prevNodes)
    val nextAnchors = groupByAnchor(nextNodes)
    val delta =
      // try aligning the common anchors...
      mapDelta(prevAnchors, nextAnchors) orElse
      // or the center of mass of the common nodes...
      mapDelta(prevNodes, nextNodes) getOrElse
      // fallback to aligning the center of mass of all nodes
      seqDelta(prevNodes.values.toSeq, nextNodes.values.toSeq)
    // we need to update the viewbox
    val withBox = SvgLenses.viewBox.modify(_ + delta)(next)
    // and the translation of the root node
    (graph composeOptional SvgLenses.translation).modify(_ + delta)(withBox)
  }
}

object SvgGraphInterpolation {
  // In the first third of the animation time interval we fade out disappearing nodes and edges.
  private val fadeOut = SvgLenses.opacity
    .semiInterpolateWith(Interpolation.double.withRight(0))
    .timespan(0, 1/3.0)

  // The new nodes and edges fade in during the last third of the animation time interval.
  private val fadeIn = SvgLenses.opacity
    .semiInterpolateWith(Interpolation.double.withLeft(0))
    .timespan(2/3.0, 1)

  private val color = (
    SvgLenses.fillColor.interpolateWith(Color.interpolation.option(_.opacify(0))) +
    SvgLenses.strokeColor.interpolateWith(Color.interpolation.option(_.opacify(0)))
  ).timespan(1/3.0, 1)

  private val thickness = SvgLenses.thickness
    .interpolateWith(Interpolation.double)
    .timespan(0, 1)

  // We move the node as a whole, since nothing inside changes position between frames.
  // Movement happens in the second third of the animation time interval.
  private val nodePosition = {
    SvgLenses.onlyFor(sel"g.node") composeOptional
    SvgLenses.groupPosition(
      SvgLenses.collectFirst(sel"text") composeOptional SvgLenses.textPosition
    )
  }.interpolateWith(Point.interpolation)
    .timespan(1/3.0, 2/3.0)

  // To move an edge, we need to move the curve and the arrow separately.
  private val edgePosition = {
    SvgLenses.onlyFor(sel"g.edge") composeLens
    SvgLenses.collectByIndex(sel"path, polygon")
  }.interpolateEachWith(
    SvgLenses.path.interpolateWith(Path.interpolation) +
    SvgLenses.polygonPoints.interpolateWith(Polyline.interpolation)
  ).timespan(1/3.0, 2/3.0)

  private val nodeOrEdge =
    nodePosition +
    edgePosition +
    SvgLenses.collectByIndex(sel"path, polygon, text").interpolateEachWith(color + thickness)

  val interpolation: Interpolation[xml.Node] =
    SvgLenses.collectFirst(sel"g.graph").interpolateWith(
      SvgLenses.collectById(sel"g.node, g.edge").interpolateEachWith(
        nodeOrEdge.option(fadeOut, fadeIn)
      )
    )
}

object SvgGraphAnimation {
  /** Prevent ugly rendering artifacts */
  private def improveRendering(svg: xml.Node): xml.Node =
    SvgLenses.attr("shape-rendering").set(Some("geometricPrecision")) andThen
    SvgLenses.attr("text-rendering").set(Some("geometricPrecision")) apply svg

  /**
   * Graphviz does not set the fill-opacity attribute on text,
   * so we copy it from the sibling path node.
   */
  private def fixTextColor(svg: xml.Node) = {
    val nodes = SvgLenses.collectAll(sel"g.node")
    val texts = SvgLenses.collectAll(sel"text")
    val nodeColor = SvgLenses.collectLast(sel"path") composeOptional SvgLenses.strokeColor
    nodes.modify { node ⇒
      val color = nodeColor.getOption(node)
      color.fold(node)(c ⇒ texts.modify(SvgLenses.fillColor.set(c))(node))
    }(svg)
  }

  /** Make the newly appeared nodes and edges thicker */
  private def accentuatePairwise(svgs: Seq[xml.Node]) = {
    val nodesAndEdges = SvgLenses.collectById(sel"g.node, g.edge")
    val thickness = SvgLenses.collectAll(sel"path, polygon") composeOptional SvgLenses.thickness

    def accentuate(prev: xml.Node, next: xml.Node) = {
      val prevNodes = nodesAndEdges.get(prev)
      nodesAndEdges.modify(_ map {
        case (id, node) if !prevNodes.contains(id) && !id.contains("-caption-") ⇒
          (id, thickness.modify(_ * 1.6)(node))
        case other ⇒ other
      })(next)
    }

    svgs.head +: svgs.sliding(2).toSeq.map {
      case Seq(prev, next) ⇒ accentuate(prev, next)
    } :+ svgs.last // this duplicates the last frame, so that it can lose thickness at the end
  }

  private def alignPairwise(svgs: Seq[xml.Node]) =
    svgs.foldLeft(Vector(svgs.head)) {
      case ((acc :+ prev), next) ⇒ acc :+ prev :+ SvgGraphAlignment.align(prev, next)
    }

  private def interpolatePairwise(svgs: Seq[xml.Node], interpolationFrames: Int) =
    svgs.head +: svgs.sliding(2).toSeq.flatMap {
      case Seq(prev, next) ⇒
        SvgGraphInterpolation.interpolation
          .sample(prev, next, interpolationFrames, inclusive = false) :+ next
    }

  def animate(interpolationFrames: Int)(svgs: Seq[xml.Node]) = {
    val preprocessed = svgs.map(improveRendering).map(fixTextColor)
    if (svgs.length < 2) preprocessed else {
      val accentuated = accentuatePairwise(preprocessed)
      val aligned = alignPairwise(accentuated)
      val maxViewBox = Rectangle.union(aligned.map(SvgLenses.viewBox.get))
      val resized = aligned.map(SvgLenses.viewBox.set(maxViewBox))
      interpolatePairwise(resized, interpolationFrames)
    }
  }
}
