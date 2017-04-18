package reftree.contrib

import monocle.{Prism, Traversal, Lens, Optional}
import reftree.core._

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
  trait Marker[A] {
    def mark(value: A): A
  }

  object Marker {
    def apply[A](f: A ⇒ A): Marker[A] = new Marker[A] {
      def mark(value: A) = f(value)
    }
    implicit val `Int Marker` = Marker[Int](x ⇒ x + 1)
    implicit val `Long Marker` = Marker[Long](x ⇒ x + 1L)
    implicit val `Char Marker` = Marker[Char] { case '?' ⇒ '!'; case _ ⇒ '?' }
    implicit val `String Marker` = Marker[String](x ⇒ x + " ")
    implicit val `Xml Marker` = Marker[xml.Node] {
      case e: xml.Elem ⇒ e % new xml.UnprefixedAttribute("marked---", "true", xml.Null)
      case x ⇒ x
    }

    import shapeless._

    implicit def `HCons Marker`[H: Marker, T <: HList]: Marker[H :: T] = Marker[H :: T] {
      case h :: t ⇒ implicitly[Marker[H]].mark(h) :: t
    }

    implicit def `Generic Marker`[A, L <: HList](
      implicit generic: Generic.Aux[A, L], hListMarker: Lazy[Marker[L]]
    ): Marker[A] = Marker[A] { value ⇒
      generic.from(hListMarker.value.mark(generic.to(value)))
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
