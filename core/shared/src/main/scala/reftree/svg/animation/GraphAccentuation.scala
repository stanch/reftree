package reftree.svg.animation

import reftree.svg.api.BaseSvgApi
import reftree.util.Optics

case class GraphAccentuation[Svg](api: BaseSvgApi[Svg]) {
  import api.svgUnzip

  private val nodesAndEdges = Optics.collectLeftByKey(api.select("g.node, g.edge"))(
    api.elementId.get(_).get
  )

  private val thickness = Optics.collectAllLeft(api.select("path, polygon")) composeOptional
    api.strokeWidth

  def accentuate(prev: Svg, next: Svg) = {
    val prevNodes = nodesAndEdges.get(prev)
    nodesAndEdges.modify(_ map {
      case (id, node) if !prevNodes.contains(id) && !id.contains("-caption-") ⇒
        (id, thickness.modify(_ * 1.6)(node))
      case other ⇒ other
    })(next)
  }

  /** Make the newly appeared nodes and edges thicker */
  def accentuatePairwise(svgs: Stream[Svg]): Stream[Svg] =
    svgs.head #:: svgs.sliding(2).toStream.map {
      case Seq(prev, next) ⇒ accentuate(prev, next)
    } #::: Stream(svgs.last) // this duplicates the last frame, so that it can lose thickness at the end
}
