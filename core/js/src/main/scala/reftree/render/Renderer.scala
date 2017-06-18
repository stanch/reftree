package reftree.render

import org.scalajs.dom.raw.DOMParser
import reftree.diagram.{Animation, Diagram}
import reftree.dot.Graph
import reftree.graph.Graphs

import org.scalajs.dom
import reftree.svg.{OptimizedGraphAnimation, DomSvgApi}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

/**
 * An interface to https://github.com/mdaines/viz.js/
 */
@js.native
@JSGlobalScope
object VizFacade extends js.Object {
  def Viz(source: String): String = js.native
}

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
 *   import org.scalajs.dom
 *
 *   val renderer = Renderer(
 *     renderingOptions = RenderingOptions(density = 75)
 *   )
 *
 *   // Conventional usage
 *   renderer
 *     .tweakRendering(_.withVerticalSpacing(2))
 *     .render(dom.document.body, Diagram(List(1)))
 *
 *   // Sweet sugar, recommended
 *   import renderer._
 *   Diagram(List(1))
 *     .render(dom.document.body, _.withVerticalSpacing(2))
 * }}}
 */
case class Renderer(
  renderingOptions: RenderingOptions = RenderingOptions(),
  animationOptions: AnimationOptions = AnimationOptions()
) { self ⇒
  /** Tweak the rendering options with the provided funciton */
  def tweakRendering(tweak: RenderingOptions ⇒ RenderingOptions) =
    copy(renderingOptions = tweak(renderingOptions))

  /** Tweak the animation options with the provided funciton */
  def tweakAnimation(tweak: AnimationOptions ⇒ AnimationOptions) =
    copy(animationOptions = tweak(animationOptions))

  private def renderSvg(graph: Graph) = (new DOMParser)
    .parseFromString(VizFacade.Viz(graph.encode), "image/svg+xml")
    .documentElement

  private def renderTo(target: dom.Node, content: dom.Node) = {
    content.asInstanceOf[dom.Element].setAttribute("width", "100%")
    content.asInstanceOf[dom.Element].setAttribute("height", "100%")
    val newTarget = target.cloneNode(deep = false)
    newTarget.appendChild(content)
    target.parentNode.replaceChild(newTarget, target)
    newTarget
  }

  private val animation = OptimizedGraphAnimation(DomSvgApi)

  /** Render a diagram to a given DOM node */
  def render(target: dom.Node, diagram: Diagram): Unit = {
    scribe.trace(s"Rendering diagram to $target")

    val graph = Graphs.graph(renderingOptions)(diagram)

    scribe.trace("Processing graphs with Viz.js...")
    val svg = renderSvg(graph)

    scribe.trace("Rendering...")
    renderTo(target, svg)
  }

  /** Render an animation to a given DOM node */
  def render(target: dom.Node, animation: Animation): Unit = {
    scribe.trace(s"Rendering animation to $target")

    val graphs = Graphs.graphs(renderingOptions, animationOptions.onionSkinLayers)(animation)

    scribe.trace("Processing graphs with Viz.js...")
    val svgs = graphs.map(renderSvg)

    scribe.trace("Preprocessing frames...")
    val frames = this.animation.animate(
      animationOptions.keyFrames, animationOptions.interpolationFrames
    )(svgs)

    scribe.trace("Starting the animation...")
    var i = 0
    var currentTarget = target
    def iteration(): Unit = {
      try {
        // we catch the IOOB exception rather than checking the bounds to avoid forcing the stream
        val currentFrame = frames(i)
        i += 1
        js.timers.setTimeout(animationOptions.delay * currentFrame.repeat)(iteration())
        currentTarget = renderTo(currentTarget, currentFrame.frame)
        if (currentFrame.repeat > 1) {
          // preprocess a few frames if we are going to be waiting
          js.timers.setTimeout(animationOptions.delay) {
            frames.take(i + currentFrame.repeat + 1).force
          }
        }
      } catch {
        case _: IndexOutOfBoundsException ⇒
          if (animationOptions.loop) {
            i = 0
            js.timers.setTimeout(animationOptions.delay)(iteration())
          }
      }
    }
    iteration()
  }

  /** Syntactic sugar for diagrams */
  implicit class DiagramRenderSyntax(diagram: Diagram) {
    def render(
      target: dom.Node,
      tweak: RenderingOptions ⇒ RenderingOptions = identity
    ) = self
      .tweakRendering(tweak)
      .render(target, diagram)
  }

  /** Syntactic sugar for animations */
  implicit class AnimationRenderSyntax(animation: Animation) {
    def render(
      target: dom.Node,
      tweakRendering: RenderingOptions ⇒ RenderingOptions = identity,
      tweakAnimation: AnimationOptions ⇒ AnimationOptions = identity
    ) = self
      .tweakRendering(tweakRendering)
      .tweakAnimation(tweakAnimation)
      .render(target, animation)
  }
}
