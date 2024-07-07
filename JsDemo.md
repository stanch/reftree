# Scala.js demo

Here is an animation of elements being appended to a list,
rendered in your browser!

```scala
val renderer = Renderer()
import renderer._

Animation.startWith(List(1)).iterate(_ :+ 2, _ :+ 3, _ :+ 4).build()
  .render(dom.document.getElementById("animation"), tweakAnimation = _.withOnionSkinLayers(2))
```

<div id="animation"></div>

<script type="text/javascript" src="js/demo.js"></script>
