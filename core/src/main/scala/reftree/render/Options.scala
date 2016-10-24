package reftree.render

import scala.concurrent.duration._

case class RenderingOptions(
  verticalSpacing: Double = 0.8,
  palette: IndexedSeq[String] = Array("#104e8b", "#228B22", "#cd5b45"),
  highlightColor: String = "#ffe4c4",
  density: Int = 100
)

case class AnimationOptions(
  keyFrameDuration: FiniteDuration = 2.seconds,
  interpolationDuration: FiniteDuration = 1.seconds,
  framesPerSecond: Int = 9,
  loop: Boolean = true,
  onionSkinLayers: Int = 0
) {
  def interpolationFrames = Math.round(interpolationDuration.toMillis * framesPerSecond / 1000.0f)

  def keyFrames = if (interpolationFrames == 0) 1 else {
    Math.round(keyFrameDuration.toMillis * framesPerSecond / 1000.0f)
  }

  def delay = if (interpolationFrames == 0) keyFrameDuration else 1.second / framesPerSecond
}
