package reftree.demo

import java.nio.file.Paths

import de.sciss.fingertree.{FingerTree, Measure}
import monocle.macros.GenLens
import reftree.diagram.{Diagram, Animation}
import reftree.render.{RenderingOptions, Renderer}
import scribe._
import scribe.handler._
import zipper.Zipper
import scala.collection.immutable._

object Queues extends App {
  Logger.root.withHandler(LogHandler(minimumLevel = Some(Level.Trace)))
  val renderer = Renderer(directory = Paths.get("images"))
  import renderer._

  Animation
    .startWith(Queue(1, 2, 3))
    .repeat(3)(_.iterate(2)(q ⇒ q :+ (q.max + 1)).iterate(2)(_.tail))
    .build(Diagram.toStringCaption(_).withAnchor("queue"))
    .render("queue")
}

object FingerTrees extends App {
  Logger.root.withHandler(LogHandler(minimumLevel = Some(Level.Trace)))
  import reftree.contrib.FingerTreeInstances._
  implicit val measure = Measure.Indexed

  val renderer = Renderer(
    renderingOptions = RenderingOptions(verticalSpacing = 2, density = 75),
    directory = Paths.get("images")
  )
  import renderer._

  Animation
    .startWith(FingerTree(1))
    .iterate(21)(t ⇒ t :+ (t.measure + 1))
    .build(Diagram(_).withCaption("Finger Tree").withAnchor("tree"))
    .render("finger")
}

object Zippers extends App {
  Logger.root.withHandler(LogHandler(minimumLevel = Some(Level.Trace)))
  import reftree.contrib.SimplifiedInstances.{list, option}
  import reftree.contrib.ZipperInstances._

  val renderer = Renderer(
    renderingOptions = RenderingOptions(density = 75),
    directory = Paths.get("images")
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

object Teaser extends App {
  Logger.root.withHandler(LogHandler(minimumLevel = Some(Level.Trace)))
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

object All extends App {
  Queues.main(Array.empty)
  FingerTrees.main(Array.empty)
  Zippers.main(Array.empty)
  Teaser.main(Array.empty)
}
