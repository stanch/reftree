package reftree.render

import java.nio.file.{Paths, Path}

import reftree.diagram.{Animation, Diagram}
import reftree.graph.Graphs
import reftree.svg.{SvgApi, SvgGraphAnimation}

/**
 * This class provides functionality for rendering diagrams and animations
 *
 * It can be used in two ways:
 *  - conventionally, via the `render` methods;
 *  - with special syntax sugar, allowing `render` calls on the diagrams/animations themselves.
 *
 * Usage examples:
 * {{{
 *   import reftree.diagram.Diagram
 *   import java.nio.file.Paths
 *
 *   val renderer = Renderer(
 *     renderingOptions = RenderingOptions(density = 75),
 *     directory = Paths.get("images", "usage")
 *   )
 *
 *   // With custom output format
 *   val renderer = Renderer(
 *     renderingOptions = RenderingOptions(density = 75),
 *     directory = Paths.get("images", "usage"),
 *     format = "ps" // Or any other format that compatible with dot -T
 *   )
 *
 *   // Conventional usage
 *   renderer
 *     .tweakRendering(_.withVerticalSpacing(2))
 *     .render("list", Diagram(List(1)))
 *
 *   // Sweet sugar, recommended
 *   import renderer._
 *   Diagram(List(1))
 *     .render("list", _.withVerticalSpacing(2))
 * }}}
 */
case class Renderer(
  renderingOptions: RenderingOptions = RenderingOptions(),
  animationOptions: AnimationOptions = AnimationOptions(),
  directory: Path = Paths.get("."),
  format: String = "png"
) { self ⇒
  /** Tweak the rendering options with the provided funciton */
  def tweakRendering(tweak: RenderingOptions ⇒ RenderingOptions) =
    copy(renderingOptions = tweak(renderingOptions))

  /** Tweak the animation options with the provided funciton */
  def tweakAnimation(tweak: AnimationOptions ⇒ AnimationOptions) =
    copy(animationOptions = tweak(animationOptions))

  /** Render a diagram to a file with the given name (do not include the extension) */
  def render(name: String, diagram: Diagram): Unit = {
    val graph = Graphs.graph(renderingOptions)(diagram)
    DotRenderer.render(
      graph,
      directory.resolve(s"$name.$format"),
      renderingOptions,
      format
    )
  }

  /** Render an animation to a GIF file with the given name (do not include the extension) */
  def render(name: String, animation: Animation): Unit = {
    val graphs = Graphs.graphs(renderingOptions, animationOptions.onionSkinLayers)(animation)
    AnimatedGifRenderer.renderAnimatedGif(
      graphs,
      SvgGraphAnimation(SvgApi).animate(animationOptions.keyFrames, animationOptions.interpolationFrames),
      directory.resolve(s"$name.gif"),
      renderingOptions,
      animationOptions
    )
  }

  /** Syntactic sugar for diagrams */
  implicit class DiagramRenderSyntax(diagram: Diagram) {
    def render(
      name: String,
      tweak: RenderingOptions ⇒ RenderingOptions = identity
    ) = self
      .tweakRendering(tweak)
      .render(name, diagram)
  }

  /** Syntactic sugar for animations */
  implicit class AnimationRenderSyntax(animation: Animation) {
    def render(
      name: String,
      tweakRendering: RenderingOptions ⇒ RenderingOptions = identity,
      tweakAnimation: AnimationOptions ⇒ AnimationOptions = identity
    ) = self
      .tweakRendering(tweakRendering)
      .tweakAnimation(tweakAnimation)
      .render(name, animation)
  }
}
