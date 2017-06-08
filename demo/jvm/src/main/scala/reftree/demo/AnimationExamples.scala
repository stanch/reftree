package reftree.demo

import java.nio.file.Paths

import de.sciss.fingertree.{FingerTree, Measure}
import com.softwaremill.quicklens._
import monocle.function.all._
import monocle.macros.GenLens
import reftree.core.RefTree
import reftree.diagram.{Diagram, Animation}
import reftree.render.{AnimationOptions, RenderingOptions, Renderer}
import zipper.Zipper

import scala.collection.immutable._

object Lists extends App {
  val renderer = Renderer(directory = Paths.get("images", "immutability", "data"))
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
  val renderer = Renderer(directory = Paths.get("images", "immutability", "data"))
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
    directory = Paths.get("images", "immutability", "data")
  )
  import renderer._

  Animation
    .startWith(FingerTree(1))
    .iterate(21)(t ⇒ t :+ (t.measure + 1))
    .build(Diagram(_).withCaption("Finger Tree").withAnchor("tree"))
    .render("finger")
}

object Zippers extends App {
  import reftree.contrib.SimplifiedInstances.{list, option}
  import reftree.contrib.ZipperInstances._

  val renderer = Renderer(
    renderingOptions = RenderingOptions(density = 75),
    directory = Paths.get("images", "immutability", "zippers")
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

  (zippers + trees).render("tree+zipper")
}

object Lenses extends App {
  import reftree.contrib.SimplifiedInstances.list
  import reftree.contrib.OpticInstances._

  val renderer = Renderer(
    animationOptions = AnimationOptions(onionSkinLayers = 2),
    directory = Paths.get("images", "immutability", "lenses")
  )
  import renderer._

  val words = each[List[String], String]
  val vowels = words composeTraversal Data.vowelTraversal
  val consonants = words composeTraversal Data.consonantTraversal

  Animation
    .startWith(List("example"))
    .iterate("sample" :: _, "specimen" :: _.tail)
    .build { words ⇒
      Diagram(OpticFocus(vowels, words)).withCaption("Vowels") +
      Diagram(OpticFocus(consonants, words)).withCaption("Consonants")
    }
    .render("vowels+consonants")
}

object Teaser extends App {
  import reftree.contrib.OpticInstances._
  import reftree.contrib.ZipperInstances._

  val renderer = Renderer(directory = Paths.get("images"))
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
    Diagram(OpticFocus(nameLens, person))
      .withCaption("Name Lens").withAnchor("lens"),
    Diagram(OpticFocus(nameLens composeTraversal Data.consonantTraversal, person))
      .withCaption("Consonant Traversal").withAnchor("lens"),
    Diagram(OpticFocus(nameLens composeTraversal Data.vowelTraversal, person))
      .withCaption("Vowel Traversal").withAnchor("lens")
  )).toNamespace("lenses")

  import reftree.contrib.SimplifiedInstances.{list, option}

  val tree = Data.Tree(1, List(Data.Tree(2), Data.Tree(3), Data.Tree(4)))

  val zippers = Animation
    .startWith(Zipper(tree).moveDownLeft)
    .iterate(_.moveRight, _.moveRight)
    .build(Diagram(_).withCaption("Zipper").withAnchor("zipper"))
    .toNamespace("zippers")

  (queues + lenses + zippers).mirror.render("teaser")
}

object Quiz extends App {
  import reftree.contrib.FingerTreeInstances._
  implicit val measure = Measure.Indexed

  val renderer = Renderer(
    renderingOptions = RenderingOptions(verticalSpacing = 2, density = 75),
    directory = Paths.get("images", "quiz")
  )
  import renderer._

  def anonymize(tree: RefTree): RefTree = tree match {
    case ref: RefTree.Ref ⇒ ref.rename("�").modify(_.children.each.value).using(anonymize)
    case other ⇒ other
  }

  def anonymize(diagram: Diagram.Single): Diagram.Single =
    diagram.modify(_.tree).using(anonymize)

  def number(i: Int) = Math.pow(2, i + 1).toInt

  Animation
    .startWith(FingerTree(1))
    .iterateWithIndex(14)((s, i) ⇒ s :+ number(i))
    .build(t ⇒ anonymize(Diagram(t).withAnchor("tree")))
    .render("1")

  Animation
    .startWith(HashSet(1))
    .iterateWithIndex(10)((s, i) ⇒ s + number(i))
    .build(s ⇒ anonymize(Diagram(s).withAnchor("set")))
    .render("2")

  Animation
    .startWith(TreeSet(1))
    .iterateWithIndex(14)((s, i) ⇒ s + number(i))
    .build(s ⇒ anonymize(Diagram(s).withAnchor("set")))
    .render("3")
}

object All extends App {
  Lists.main(Array.empty)
  Queues.main(Array.empty)
  FingerTrees.main(Array.empty)
  Zippers.main(Array.empty)
  Lenses.main(Array.empty)
  Quiz.main(Array.empty)
  Teaser.main(Array.empty)
}
