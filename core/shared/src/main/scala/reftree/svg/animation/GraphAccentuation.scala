package reftree.svg.animation

import reftree.svg.api.BaseSvgApi
import reftree.util.Optics

import scala.collection.compat.immutable.LazyList

case class GraphAccentuation[Svg](api: BaseSvgApi[Svg]) {
  import api.svgUnzip

  private val nodesAndEdges = Optics.collectLeftByKey(api.select("g.node, g.edge"))(
    api.elementId.get(_).get
  )

  private val thickness = Optics.collectAllLeft(api.select("path, polygon")) composeOptional
    api.strokeWidth

  def accentuate(prev: Svg, next: Svg): Svg = {
    val prevNodes = nodesAndEdges.get(prev)
    nodesAndEdges.modify(_ map {
      case (id, node) if !prevNodes.contains(id) && !id.contains("-caption-") =>
        (id, thickness.modify(_ * 1.6)(node))
      case other => other
    })(next)
  }

  /** Make the newly appeared nodes and edges thicker */
  def accentuatePairwise(svgs: LazyList[Svg]): LazyList[Svg] =
    svgs.head #:: svgs.sliding(2).to(LazyList).flatMap {
      case Seq(prev, next, _@_*) => Some(accentuate(prev, next))
      case Seq(prev) => Some(prev)
      case _ => None
    } #::: LazyList(svgs.last) // this duplicates the last frame, so that it can lose thickness at the end
}
