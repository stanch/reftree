package reftree.demo

import java.nio.file.Paths

import de.sciss.fingertree.{FingerTree, Measure}
import monocle.function.all._
import monocle.macros.GenLens
import monocle.std.list._
import reftree.diagram.{Diagram, Animation}
import reftree.render.{AnimationOptions, RenderingOptions, Renderer}
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
    .build(Diagram(_).withCaption("Zipper").withAnchor("zipper"))
    .toNamespace("zipper")

  val trees = movement
    .build(z ⇒ Diagram(ZipperFocus(z, Data.simpleTree)).withCaption("Tree").withAnchor("tree"))
    .toNamespace("tree")

  (zippers addInParallel trees).render("tree+zipper")
}

object Lenses extends App {
  import reftree.contrib.SimplifiedInstances.list
  import reftree.contrib.LensInstances._

  val renderer = Renderer(
    animationOptions = AnimationOptions(onionSkinLayers = 2),
    directory = Paths.get("images", "lenses")
  )
  import renderer._

  val words = each[List[String], String]
  val vowels = words composeTraversal Data.vowelTraversal
  val consonants = words composeTraversal Data.consonantTraversal

  Animation
    .startWith(List("example"))
    .iterate("sample" :: _, "specimen" :: _.tail)
    .build { words ⇒
      Diagram(LensFocus(vowels, words)).withCaption("Vowels") +
      Diagram(LensFocus(consonants, words)).withCaption("Consonants")
    }
    .render("vowels+consonants")
}

object Teaser extends App {
  import reftree.contrib.LensInstances._

  val renderer = Renderer(directory = Paths.get("images", "usage"))
  import renderer._

  val queues = Animation
    .startWith(Queue(1))
    .iterate(_ :+ 2, _.tail)
    .build(Diagram.toStringCaption(_).withAnchor("queue"))
    .toNamespace("queues")

  case class Person(name: String)
  val person = Person("Maggie")
  val nameLens = GenLens[Person](_.name)

  val lenses = Animation(Seq(
    Diagram(LensFocus(nameLens, person))
      .withCaption("Name Lens").withAnchor("lens"),
    Diagram(LensFocus(nameLens composeTraversal Data.consonantTraversal, person))
      .withCaption("Consonant Traversal").withAnchor("lens"),
    Diagram(LensFocus(nameLens composeTraversal Data.vowelTraversal, person))
      .withCaption("Vowel Traversal").withAnchor("lens")
  )).toNamespace("lenses")

  val tree = Data.Tree(1, List(Data.Tree(2), Data.Tree(3), Data.Tree(4)))
  import reftree.contrib.SimplifiedInstances.{list, option, zipper}

  val zippers = Animation
    .startWith(Zipper(tree).moveDownLeft)
    .iterate(_.moveRight, _.moveRight)
    .build(Diagram(_).withCaption("Zipper").withAnchor("zipper"))
    .toNamespace("zippers")

  (queues addInParallel lenses addInParallel zippers).mirror.render("teaser")
}

object All extends App {
  Lists.main(Array.empty)
  Queues.main(Array.empty)
  FingerTrees.main(Array.empty)
  Zippers.main(Array.empty)
  Lenses.main(Array.empty)
  Teaser.main(Array.empty)
}
