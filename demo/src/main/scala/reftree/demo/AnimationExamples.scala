package reftree.demo

import java.nio.file.Paths

import de.sciss.fingertree.{FingerTree, Measure}
import reftree.diagram.{Diagram, Animation}
import reftree.render.{RenderingOptions, Renderer}
import zipper.Zipper

import scala.collection.immutable._

object Lists extends App {
  val renderer = Renderer(directory = Paths.get("images", "data"))
  import renderer._

  Animation
    .startWith(List(1))
    .iterate(_ :+ 2, _ :+ 3, _ :+ 4)
    .build()
    .render("list-append", tweakAnimation = _.withOnionSkinLayers(3))

  Animation
    .startWith(List(1))
    .iterate(2 :: _, 3 :: _, 4 :: _)
    .build()
    .render("list-prepend")
}

object Queues extends App {
  val renderer = Renderer(directory = Paths.get("images", "data"))
  import renderer._

  Animation
    .startWith(Queue(1, 2, 3))
    .repeat(3)(_.iterate(2)(q ⇒ q :+ (q.max + 1)).iterate(2)(_.tail))
    .build(Diagram.toStringCaption(_).withAnchor("queue"))
    .render("queue")
}

object FingerTrees extends App {
  import reftree.contrib.FingerTreeInstances._
  implicit val measure = Measure.Indexed

  val renderer = Renderer(
    renderingOptions = RenderingOptions(verticalSpacing = 2, density = 75),
    directory = Paths.get("images", "data")
  )
  import renderer._

  Animation
    .startWith(FingerTree(1))
    .iterate(21)(t ⇒ t :+ (t.measure + 1))
    .build(Diagram(_).withCaption("Finger Tree").withAnchor("tree"))
    .render("finger")
}

object Zippers extends App {
  import reftree.contrib.ZipperInstances._
  import reftree.contrib.SimplifiedInstances.{list, option, zipper}

  val renderer = Renderer(
    renderingOptions = RenderingOptions(density = 75),
    directory = Paths.get("images", "zippers")
  )
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

  val zippers = movement
    .build(Diagram(_).withCaption("Zipper").withAnchor("zipper").withColor(2))
    .toNamespace("zipper")

  val trees = movement
    .build(z ⇒ Diagram(ZipperFocus(z, Data.simpleTree)).withCaption("Tree").withAnchor("tree"))
    .toNamespace("tree")

  (zippers addInParallel trees).render("tree+zipper")
}
