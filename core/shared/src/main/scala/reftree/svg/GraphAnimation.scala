package reftree.svg

import reftree.svg.animation._
import reftree.svg.api.{BaseSvgApi, OptimizedSvgApi, SvgWrapper}

import scala.collection.compat.immutable.LazyList

case class GraphAnimation[Svg](api: BaseSvgApi[Svg]) {
  val alignment = GraphAlignment(api)
  val cleanup = GraphCleanup(api)
  val accentuation = GraphAccentuation(api)
  val interpolation = GraphInterpolation(api)

  def animate(keyFrames: Int, interpolationFrames: Int)(svgs: Seq[Svg]): LazyList[Frame[Svg]] = {
    if (svgs.length < 2) {
      cleanup.cleanup(svgs.to(LazyList)).map(Frame(_))
    } else {
      scribe.trace("Aligning...")
      val aligned = alignment.alignPairwise(svgs)
      val resized = alignment.unifyDimensions(aligned)

      scribe.trace("Cleaning up...")
      val clean = cleanup.cleanup(resized)

      scribe.trace("Accentuating differences between adjacent frames...")
      val accentuated = accentuation.accentuatePairwise(clean)

      scribe.trace("Interpolating...")
      interpolation.interpolatePairwise(accentuated, keyFrames, interpolationFrames)
    }
  }
}

case class OptimizedGraphAnimation[Svg](api: BaseSvgApi[Svg]) {
  val optimizedApi = OptimizedSvgApi(api)
  val animation = GraphAnimation(optimizedApi)

  def animate(keyFrames: Int, interpolationFrames: Int)(svgs: Seq[Svg]): LazyList[Frame[Svg]] = {
    scribe.trace("Optimizing...")
    val wrapped = svgs.map(SvgWrapper.wrap(api))

    val wrappedFrames = animation.animate(keyFrames, interpolationFrames)(wrapped)

    scribe.trace("Optimizing...")
    wrappedFrames.map(_.map(_.unwrap))
  }
}
