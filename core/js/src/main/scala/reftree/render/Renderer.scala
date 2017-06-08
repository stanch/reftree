package reftree.render

import org.scalajs.dom.raw.DOMParser
import reftree.diagram.Diagram
import reftree.graph.Graphs

import org.scalajs.dom

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

  /** Render a diagram to a given DOM node */
  def render(target: dom.Node, diagram: Diagram): Unit = {
    val graph = Graphs.graph(renderingOptions)(diagram)
    val svg = VizFacade.Viz(graph.toString)
    val dom = (new DOMParser).parseFromString(svg, "image/svg+xml").documentElement
    target.parentNode.replaceChild(dom, target)
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
}
