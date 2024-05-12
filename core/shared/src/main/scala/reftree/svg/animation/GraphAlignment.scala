package reftree.svg.animation

import reftree.geometry.{Point, Rectangle}
import reftree.svg.api.BaseSvgApi
import reftree.util.Optics

import scala.collection.compat.immutable.LazyList

case class GraphAlignment[Svg](api: BaseSvgApi[Svg]) {
  import api.svgUnzip

  private val graph = Optics.collectFirst(api.select("g.graph"))

  private val nodes = Optics.collectLeftByKey(api.select("g.node"))(
    api.elementId.get(_).get
  )

  private val nodeAnchor =
    Optics.collectFirst(api.anchors) composeOptional api.anchorTitle

  private val nodePosition =
    Optics.collectFirst(api.texts) composeOptional api.realTextPosition

  private def groupByAnchor(nodes: Map[String, Svg]) =
    nodes.values.groupBy(nodeAnchor.getOption(_).flatten) flatMap {
      case (Some(anchor), group) => Some((anchor, group.head))
      case _ => None
    }

  private def mapDelta(prev: Map[String, Svg], next: Map[String, Svg]) = {
    val common = prev.keySet & next.keySet
    if (common.isEmpty) None else {
      val deltas = common map { id =>
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

  /** Align a sequence of animation frames pairwise */
  def alignPairwise(svgs: Seq[Svg]): Vector[Svg] =
    svgs.foldLeft(Vector(svgs.head)) {
      case (acc :+ prev, next) => acc :+ prev :+ align(prev, next)
      case (Vector(prev), next) => Vector(prev) :+ align(prev, next)
      case (_, next) => Vector(next)
    }

  /** Set the viewbox of all frames to the smallest common viewbox */
  def unifyDimensions(svgs: Vector[Svg]): LazyList[Svg] = {
    val maxViewBox = Rectangle.union(svgs.map(api.viewBox.getOption(_).get))
    svgs.to(LazyList).map(api.viewBox.set(maxViewBox))
  }
}
