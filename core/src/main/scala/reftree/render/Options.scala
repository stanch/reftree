package reftree.render

import reftree.geometry.Color
import com.softwaremill.quicklens._

import scala.concurrent.duration._

case class RenderingOptions(
  verticalSpacing: Double = 0.8,
  palette: IndexedSeq[Color] = Array(
    Color.fromRgbString("#104e8b"),
    Color.fromRgbString("#228b22"),
    Color.fromRgbString("#cd5b45")
  ),
  highlightColor: Color = Color.fromRgbString("#ffe4c4"),
  density: Int = 100
) {
  def withVerticalSpacing(spacing: Double) = copy(verticalSpacing = spacing)
  def withDensity(density: Int) = copy(density = density)
  def mapPalette(f: Color ⇒ Color) = this.modify(_.palette.each).using(f)
}

case class AnimationOptions(
  keyFrameDuration: FiniteDuration = 2.seconds,
  interpolationDuration: FiniteDuration = 1.seconds,
  framesPerSecond: Int = 9,
  loop: Boolean = true,
  onionSkinLayers: Int = 0
) {
  def withOnionSkinLayers(layers: Int) = copy(onionSkinLayers = layers)

  def interpolationFrames = Math.round(interpolationDuration.toMillis * framesPerSecond / 1000.0f)

  def keyFrames = if (interpolationFrames == 0) 1 else {
    Math.round(keyFrameDuration.toMillis * framesPerSecond / 1000.0f)
  }

  def delay = if (interpolationFrames == 0) keyFrameDuration else 1.second / framesPerSecond
}
