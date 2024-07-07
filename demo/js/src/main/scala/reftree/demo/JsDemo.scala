package reftree.demo

import reftree.diagram.Animation
import reftree.render.Renderer
import org.scalajs.dom

object JsDemo {
  def main(args: Array[String]): Unit = {
    val renderer = Renderer()
    import renderer._

    Animation.startWith(List(1)).iterate(_ :+ 2, _ :+ 3, _ :+ 4).build()
      .render(dom.document.getElementById("animation"), tweakAnimation = _.withOnionSkinLayers(2))
  }
}
