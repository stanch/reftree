package reftree.svg

import reftree.geometry._
import reftree.util.Optics

case class SvgGraphAlignment[Svg](api: SvgApi[Svg]) {
  import api.svgUnzip

  private val graph = Optics.collectFirst(api.select("g.graph"))

  private val nodes = Optics.collectLeftByKey(api.select("g.node"))(
    api.elementId.get(_).get
  )

  private val nodeAnchor = Optics.collectFirst(api.select("a")) composePrism
    api.anchors composeLens
    api.anchorTitle

  private val nodePosition = Optics.collectFirst(api.select("text")) composePrism
    api.texts composeLens
    api.textPosition

  private def groupByAnchor(nodes: Map[String, Svg]) =
    nodes.values.groupBy(nodeAnchor.getOption(_).flatten) flatMap {
      case (Some(anchor), group) ⇒ Some((anchor, group.head))
      case _ ⇒ None
    }

  private def mapDelta(prev: Map[String, Svg], next: Map[String, Svg]) = {
    val common = prev.keySet & next.keySet
    if (common.isEmpty) None else {
      val deltas = common map { id ⇒
        nodePosition.getOption(prev(id)).get - nodePosition.getOption(next(id)).get
      }
      Some(Point.mean(deltas.toSeq))
    }
  }

  private def seqDelta(prev: Seq[Svg], next: Seq[Svg]) = {
    Point.mean(prev.map(nodePosition.getOption(_).get)) -
      Point.mean(next.map(nodePosition.getOption(_).get))
  }

  /** Align two adjacent animation frames */
  def align(prev: Svg, next: Svg) = {
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
    val withBox = api.viewBox.modify(_ + delta)(next)
    // and the translation of the root node
    (graph composeOptional api.translation).modify(_ + delta)(withBox)
  }
}

case class SvgGraphInterpolation[Svg](api: SvgApi[Svg]) {
  import api.svgUnzip

  // In the first third of the animation time interval we fade out disappearing nodes and edges.
  private val fadeOut = api.opacity
    .semiInterpolateWith(Interpolation.double.withRight(0))
    .timespan(0, 1/3.0)

  // The new nodes and edges fade in during the last third of the animation time interval.
  private val fadeIn = api.opacity
    .semiInterpolateWith(Interpolation.double.withLeft(0))
    .timespan(2/3.0, 1)

  private val color = (
    api.fillColor.interpolateWith(Color.interpolation.option(_.opacify(0))) +
    api.strokeColor.interpolateWith(Color.interpolation.option(_.opacify(0)))
  ).timespan(1/3.0, 1)

  private val thickness = api.strokeWidth
    .interpolateWith(Interpolation.double)
    .timespan(0, 1)

  // We move the node as a whole, since nothing inside changes position between frames.
  // Movement happens in the second third of the animation time interval.
  private val nodePosition = {
    Optics.only(api.select("g.node")) composeOptional
    api.groupPosition(
      Optics.collectFirst(api.select("text")) composePrism api.texts composeLens api.textPosition
    )
  }.interpolateWith(Point.interpolation)
    .timespan(1/3.0, 2/3.0)

  // To move an edge, we need to move the curve and the arrow separately.
  private val edgePosition = {
    Optics.only(api.select("g.edge")) composeLens
    Optics.collectLeftByIndex(api.select("path, polygon"))
  }.interpolateEachWith(
    (api.paths composeLens api.pathPath).interpolateWith(Path.interpolation(100)) +
    (api.polygons composeLens api.polygonPoints).interpolateWith(Polyline.interpolation)
  ).timespan(1/3.0, 2/3.0)

  private val nodeOrEdge =
    nodePosition +
    edgePosition +
    Optics.collectLeftByIndex(api.select("path, polygon, text")).interpolateEachWith(color + thickness)

  val interpolation: Interpolation[Svg] =
    Optics.collectFirst(api.select("g.graph")).interpolateWith(
      Optics.collectLeftByKey(api.select("g.node, g.edge"))(
        api.elementId.get(_).get
      ).interpolateEachWith(
        nodeOrEdge.option(fadeOut, fadeIn)
      )
    )
}

case class SvgGraphAnimation[Svg](api: SvgApi[Svg]) {
  import api.svgUnzip

  /** Prevent ugly rendering artifacts */
  private def improveRendering(svg: Svg): Svg =
    api.shapeRendering.set(Some("geometricPrecision")) andThen
    api.textRendering.set(Some("geometricPrecision")) apply svg

  /**
   * Graphviz does not set the fill-opacity attribute on text,
   * so we copy it from the sibling path node.
   */
  private def fixTextColor(svg: Svg) = {
    val nodes = Optics.collectAllLeft(api.select("g.node"))
    val texts = Optics.collectAllLeft(api.select("text"))
    val nodeColor = Optics.collectLast(api.select("path")) composeLens api.strokeColor
    nodes.modify { node ⇒
      val color = nodeColor.getOption(node)
      color.fold(node)(c ⇒ texts.modify(api.fillColor.set(c))(node))
    }(svg)
  }

  /** Make the newly appeared nodes and edges thicker */
  private def accentuatePairwise(svgs: Seq[Svg]) = {
    val nodesAndEdges = Optics.collectLeftByKey(api.select("g.node, g.edge"))(
      api.elementId.get(_).get
    )
    val thickness = Optics.collectAllLeft(api.select("path, polygon")) composeLens api.strokeWidth

    def accentuate(prev: Svg, next: Svg) = {
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

  private def alignPairwise(svgs: Seq[Svg]) =
    svgs.foldLeft(Vector(svgs.head)) {
      case ((acc :+ prev), next) ⇒ acc :+ prev :+ SvgGraphAlignment(api).align(prev, next)
    }

  private def interpolatePairwise(svgs: Seq[Svg], keyFrames: Int, interpolationFrames: Int) =
    Seq.fill(keyFrames)(svgs.head) ++ svgs.sliding(2).toSeq.flatMap {
      case Seq(prev, next) ⇒
        SvgGraphInterpolation(api).interpolation
          .sample(prev, next, interpolationFrames, inclusive = false) ++ Seq.fill(keyFrames)(next)
    }

  def animate(keyFrames: Int, interpolationFrames: Int)(svgs: Seq[Svg]) = {
    val preprocessed = svgs.map(improveRendering).map(fixTextColor)
    if (svgs.length < 2) preprocessed else {
      val accentuated = accentuatePairwise(preprocessed)
      val aligned = alignPairwise(accentuated)
      val maxViewBox = Rectangle.union(aligned.map(api.viewBox.get))
      val resized = aligned.map(api.viewBox.set(maxViewBox))
      interpolatePairwise(resized, keyFrames, interpolationFrames)
    }
  }
}
