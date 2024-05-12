package reftree.svg.animation

import reftree.geometry._
import reftree.svg.api.BaseSvgApi
import reftree.util.Optics

import scala.collection.compat.immutable.LazyList

/** An animation frame, possibly repeated several times */
case class Frame[Svg](frame: Svg, repeat: Int = 1) {
  def map[A](f: Svg => A) = copy(f(frame))
}

case class GraphInterpolation[Svg](api: BaseSvgApi[Svg]) {
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
    api.select("g.node") composeOptional
    api.groupPosition(
      Optics.collectFirst(api.texts) composeOptional api.realTextPosition
    )
  }.interpolateWith(Point.interpolation)
    .timespan(1/3.0, 2/3.0)

  // To move an edge, we need to move the curve and the arrow separately.
  private val edgePosition = {
    api.select("g.edge") composeLens
    Optics.collectLeftByIndex(api.select("path, polygon"))
  }.interpolateEachWith(
    api.realPathPath.interpolateWith(Path.interpolation(100)) +
    api.realPolygonPoints.interpolateWith(Polyline.interpolation)
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

  def interpolatePairwise(
    svgs: LazyList[Svg],
    keyFrames: Int,
    interpolationFrames: Int
  ): LazyList[Frame[Svg]] =
    Frame(svgs.head, keyFrames) #:: svgs.sliding(2).to(LazyList).flatMap {
      case Seq(prev, next, _@_*) =>
        interpolation.sample(prev, next, interpolationFrames, inclusive = false)
          .map(Frame(_)) #::: LazyList(Frame(next, keyFrames))
      case Seq(prev) =>
        LazyList(Frame(prev, keyFrames))
      case _ =>
        LazyList.empty
    }
}
