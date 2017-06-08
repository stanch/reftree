package reftree.render

import org.scalajs.dom.raw.DOMParser
import reftree.diagram.{Animation, Diagram}
import reftree.dot.Graph
import reftree.graph.Graphs

import org.scalajs.dom
import reftree.svg.{SvgApi, SvgGraphAnimation}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope
import scala.scalajs.js.timers.SetIntervalHandle

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
    .parseFromString(VizFacade.Viz(graph.toString), "image/svg+xml")
    .documentElement

  private def renderTo(target: dom.Node, content: dom.Node) = {
    val newTarget = target.cloneNode(deep = false)
    newTarget.appendChild(content)
    target.parentNode.replaceChild(newTarget, target)
    newTarget
  }

  /** Render a diagram to a given DOM node */
  def render(target: dom.Node, diagram: Diagram): Unit = {
    val graph = Graphs.graph(renderingOptions)(diagram)
    val svg = renderSvg(graph)
    renderTo(target, svg)
  }

  /** Render an animation to a given DOM node */
  def render(target: dom.Node, animation: Animation): Unit = {
    val graphs = Graphs.graphs(renderingOptions, animationOptions.onionSkinLayers)(animation)
    val svgs = graphs.map(renderSvg)
    val frames = SvgGraphAnimation(SvgApi).animate(
      animationOptions.keyFrames, animationOptions.interpolationFrames
    )(svgs)
    var i = 0
    var currentTarget = target
    lazy val interval: SetIntervalHandle = js.timers.setInterval(animationOptions.delay) {
      try {
        currentTarget = renderTo(currentTarget, frames(i))
        i += 1
      } catch {
        case _: IndexOutOfBoundsException ⇒
          if (animationOptions.loop) i = 0
          else js.timers.clearInterval(interval)
      }
    }
    interval
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
