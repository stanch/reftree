package reftree.demo

import reftree.diagram.Animation
import reftree.render.Renderer

import scala.scalajs.js
import org.scalajs.dom

object JsDemo extends js.JSApp {
  def main(): Unit = {
    val renderer = Renderer()
    import renderer._

    Animation.startWith(List(1)).iterate(_ :+ 2, _ :+ 3, _ :+ 4).build()
      .render(dom.document.getElementById("animation"), tweakAnimation = _.withOnionSkinLayers(2))
  }
}
