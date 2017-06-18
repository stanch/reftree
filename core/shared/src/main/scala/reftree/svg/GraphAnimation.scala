package reftree.svg

import reftree.svg.animation._
import reftree.svg.api.{SvgWrapper, OptimizedSvgApi, BaseSvgApi}

case class GraphAnimation[Svg](api: BaseSvgApi[Svg]) {
  import GraphAnimation._

  val alignment = GraphAlignment(api)
  val cleanup = GraphCleanup(api)
  val accentuation = GraphAccentuation(api)
  val interpolation = GraphInterpolation(api)

  def animate(keyFrames: Int, interpolationFrames: Int)(svgs: Seq[Svg]): Stream[Frame[Svg]] = {
    if (svgs.length < 2) {
      cleanup.cleanup(svgs.toStream).map(Frame(_, 1))
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
  import GraphAnimation._

  val optimizedApi = OptimizedSvgApi(api)
  val animation = GraphAnimation(optimizedApi)

  def animate(keyFrames: Int, interpolationFrames: Int)(svgs: Seq[Svg]): Stream[Frame[Svg]] = {
    scribe.trace("Optimizing...")
    val wrapped = svgs.map(SvgWrapper.wrap(api))

    val wrappedFrames = animation.animate(keyFrames, interpolationFrames)(wrapped)

    scribe.trace("Optimizing...")
    wrappedFrames.map(_.map(_.unwrap))
  }
}

object GraphAnimation {
  /** An animation frame, possibly repeated several times */
  case class Frame[Svg](frame: Svg, repeat: Int) {
    def map[A](f: Svg ⇒ A) = copy(f(frame))
  }
}
