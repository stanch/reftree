package reftree.demo

import java.nio.file.Paths

import com.softwaremill.quicklens._
import de.sciss.fingertree.{FingerTree, Measure}
import reftree.contrib.ZipperInstances._
import reftree.contrib.FingerTreeInstances._
import reftree.diagram.{Diagram, Animation}
import reftree.render.{RenderingOptions, Renderer}
import zipper.Zipper

import scala.collection.immutable._

object Lists extends App {
  val renderer = Renderer(directory = Paths.get("images", "data"))

  renderer.modify(_.animationOptions.onionSkinLayers).setTo(3).render(
    "list-append",
    Animation.startWith(List(1)).iterate(_ :+ 2, _ :+ 3, _ :+ 4).build()
  )

  renderer.render(
    "list-prepend",
    Animation.startWith(List(1)).iterate(2 :: _, 3 :: _, 4 :: _).build()
  )
}

object Queues extends App {
  val renderer = Renderer(directory = Paths.get("images", "data"))
  import renderer._

  Animation
    .startWith(Queue(1, 2, 3))
    .repeat(3)(_.iterate(2)(q ⇒ q :+ (q.max + 1)).iterate(2)(_.tail))
    .build(Diagram.toStringLabel(_).withAnchor("queue"))
    .render("queue")
}

object FingerTrees extends App {
  val renderer = Renderer(
    renderingOptions = RenderingOptions(verticalSpacing = 2, density = 75),
    directory = Paths.get("images", "data")
  )
  import renderer._

  implicit val measure = Measure.Indexed

  Animation
    .startWith(FingerTree(1))
    .iterate(21)(t ⇒ t :+ (t.measure + 1))
    .build(Diagram(_).withLabel("Finger Tree").withAnchor("tree"))
    .render("finger")
}

object Zippers extends App {
  import reftree.contrib.SimplifiedInstances.{list, option, zipper}
  val renderer = Renderer(directory = Paths.get("images", "zippers"))
  import renderer._

  val movement = Animation
    .startWith(Zipper(Data.simpleTree))
    .iterate(
      _.moveDownLeft,
      _.moveRight, _.moveRight, _.moveRight,
      _.moveDownLeft,
      _.moveRight, _.moveLeft,
      _.top.get,
      _.moveLeft, _.moveLeft, _.moveLeft,
      _.top.get
    )

  val trees = movement
    .build(z ⇒ Diagram(ZipperFocus(z, Data.simpleTree)).withLabel("Tree").withAnchor("tree"))
    .toNamespace("tree")

  val zippers = movement
    .build(Diagram(_).withLabel("Zipper").withAnchor("zipper").withColor(2))
    .toNamespace("zipper")

  (trees addInParallel zippers).render("tree+zipper")
}
