package reftree.render

import reftree.geometry.Color
import com.softwaremill.quicklens._

import scala.concurrent.duration._

/**
 * Options for rendering static diagrams
 *
 * @param verticalSpacing vertical spacing to set for Graphviz
 * @param palette a sequence of colors to be used
 * @param font the font for text rendering
 * @param density the desired image density, in pixels per inch
 */
case class RenderingOptions(
  verticalSpacing: Double = 0.8,
  palette: IndexedSeq[Color] = Array(
    Color.fromRgbaString("#104e8b"),
    Color.fromRgbaString("#228b22"),
    Color.fromRgbaString("#cd5b45")
  ),
  font: String = "Source Code Pro",
  density: Int = 100
) {
  def withVerticalSpacing(spacing: Double) = copy(verticalSpacing = spacing)
  def withFont(font: String) = copy(font = font)
  def withDensity(density: Int) = copy(density = density)
  def mapPalette(f: Color ⇒ Color) = this.modify(_.palette.each).using(f)
}

/**
 * Options for rendering animations
 *
 * @param keyFrameDuration the duration for key frames
 * @param interpolationDuration the duration for interpolation segments between the key frames
 *                              set to `Duration.Zero` to disable interpolation
 * @param framesPerSecond the rate to use for interpolation (ignored when interpolation is off)
 * @param loop whether the animation should be looped infinitely
 * @param onionSkinLayers set to a positive number `n` to add `n` previous frames to each frame,
 *                        with reduced opacity
 */
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
