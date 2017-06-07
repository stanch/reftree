package reftree.contrib

import monocle.{Prism, Traversal, Lens, Optional}
import reftree.core._
import shapeless.ProductTypeClassCompanion

import scala.annotation.implicitNotFound

/**
 * [[ToRefTree]] instances for [[OpticInstances.OpticFocus]] that show optics together with their focuses
 */
object OpticInstances {
  /** A class to represent a [[Traversal]] together with its target */
  case class OpticFocus[A, B](traversal: Traversal[A, B], target: A)

  object OpticFocus {
    /** Construct an [[OpticFocus]] from a lens and its target */
    def apply[A, B](lens: Lens[A, B], target: A): OpticFocus[A, B] =
      OpticFocus(lens.asTraversal, target)

    /** Construct an [[OpticFocus]] from an optional and its target */
    def apply[A, B](optional: Optional[A, B], target: A): OpticFocus[A, B] =
      OpticFocus(optional.asTraversal, target)

    /** Construct an [[OpticFocus]] from a prism and its target */
    def apply[A, B](prism: Prism[A, B], target: A): OpticFocus[A, B] =
      OpticFocus(prism.asTraversal, target)
  }

  /** A typeclass for marking values of a certain type */
  @implicitNotFound("Could not find a way to mark a value of type ${A}")
  case class Marker[A](mark: A ⇒ A)

  object Marker extends ProductTypeClassCompanion[Marker] {
    implicit val `Int Marker` = new Marker[Int](x ⇒ x + 1)
    implicit val `Long Marker` = new Marker[Long](x ⇒ x + 1L)
    implicit val `Double Marker` = new Marker[Double](x ⇒ x + 1.0)
    implicit val `Char Marker` = new Marker[Char]({ case '?' ⇒ '!'; case _ ⇒ '?' })
    implicit val `String Marker` = new Marker[String](x ⇒ x + " ")

    import shapeless._

    object typeClass extends ProductTypeClass[Marker] {
      def emptyProduct: Marker[HNil] =
        new Marker(identity)

      def product[H, T <: HList](ch: Marker[H], ct: Marker[T]): Marker[H :: T] =
        new Marker(value ⇒ ch.mark(value.head) :: ct.mark(value.tail))

      def project[F, G](instance: ⇒ Marker[G], to: F ⇒ G, from: G ⇒ F): Marker[F] =
        new Marker(value ⇒ from(instance.mark(to(value))))
    }
  }

  implicit def `OpticFocus RefTree`[A, B](
    implicit refTreeA: ToRefTree[A],
    refTreeB: ToRefTree[B],
    marker: Marker[B]
  ): ToRefTree[OpticFocus[A, B]] = ToRefTree[OpticFocus[A, B]] { value ⇒
    // collect the trees of the data the optic focuses on
    val foci = value.traversal.getAll(value.target).map(_.refTree).toSet

    // modify the target using the optic and the marker function
    val marked = value.traversal.modify(marker.mark)(value.target)

    def matching(tree: RefTree, pattern: RefTree): Boolean = (tree, pattern) match {
      case (RefTree.Null(_), RefTree.Null(_)) ⇒ true
      case (RefTree.Val(x, _, _), RefTree.Val(y, _, _)) ⇒ x == y
      case (x: RefTree.Ref, y: RefTree.Ref) ⇒
        x.name == y.name &&
          x.children.map(_.name) == y.children.map(_.name) &&
          (x.children.map(_.value) zip y.children.map(_.value))
            .forall { case(a, b) ⇒ matching(a, b) }
      case _ ⇒ false
    }

    def inner(original: RefTree, marked: RefTree): RefTree = (original, marked) match {
      case _ if foci.exists(matching(original, _)) && original != marked ⇒
        // the tree matches one of the foci and changed after marking, it must be it!
        original.withHighlight(true)

      case (x: RefTree.Ref, y: RefTree.Ref) ⇒
        // recurse
        val children = (x.children zip y.children) map {
          case (cx, cy) ⇒ cx.copy(value = inner(cx.value, cy.value))
        }
        x.copy(children = children)

      case _ ⇒ original
    }

    // compare the RefTrees before and after modification
    inner(value.target.refTree, marked.refTree)
  }
}
