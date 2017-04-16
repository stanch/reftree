package reftree.core

import scala.annotation.implicitNotFound
import scala.collection.immutable.CollectionInstances

/**
 * A [[RefTree]] represents the object tree of an immutable data structure.
 * Mutable data can be represented as well, as long as it is acyclic.
 *
 * A [[RefTree]] has three subtypes:
 *  - [[RefTree.Ref]] represents an object ([[AnyRef]]) with identity and a sequence of fields;
 *  - [[RefTree.Val]] represents a value ([[AnyVal]]);
 *  - [[RefTree.Null]] is a special case to represent `null`.
 *
 * This type is mainly geared towards visualization of structural sharing,
 * however in some cases the representation can be simplified for the benefit of the visualization.
 * For example, [[reftree.contrib.SimplifiedInstances]] contains “black box” representations
 * of [[List]], [[Seq]] and [[Map]] that do not expose the internal structure.
 *
 * A [[RefTree]] for a type `A` can be obtained using the [[ToRefTree]] typeclass.
 */
sealed trait RefTree {
  /**
   * The identifier for this tree.
   * Note that this is unique only for [[RefTree.Ref]]. For [[RefTree.Null]] it is always `null`,
   * and value ([[RefTree.Val]]) identifiers are shared across all equivalent values.
   */
  def id: String

  /** Whether this tree should be highlighted during visualization */
  def highlight: Boolean

  /** Add or remove highlighting */
  def withHighlight(highlight: Boolean) = this match {
    case tree: RefTree.Val ⇒ tree.copy(highlight = highlight)
    case tree: RefTree.Null ⇒ tree.copy(highlight = highlight)
    case tree: RefTree.Ref ⇒ tree.copy(highlight = highlight)
  }

  /** Convert to a field usable in other trees */
  def toField: RefTree.Ref.Field = RefTree.Ref.Field(this)
}

object RefTree {
  /** A special case [[RefTree]] for `null` values. */
  case class Null(highlight: Boolean = false) extends RefTree {
    def id = "null"
  }

  /** A [[RefTree]] for a value */
  case class Val(
    value: AnyVal,
    hint: Option[Val.Hint],
    highlight: Boolean
  ) extends RefTree {
    def id = value.toString

    /* Add a visualization hint */
    def withHint(hint: Val.Hint) = copy(hint = Some(hint))
  }

  object Val {
    /** Special visualization hints */
    sealed trait Hint
    case object Bin extends Hint
    case object Hex extends Hint

    /** Construct a [[RefTree]] for a value */
    def apply(value: AnyVal): Val = Val(value, None, highlight = false)
  }

  /**
   * A [[RefTree]] for an object
   *
   * Use the companion `apply` method to construct this tree.
   */
  case class Ref private (
    name: String,
    id: String,
    children: Seq[Ref.Field],
    highlight: Boolean
  ) extends RefTree {
    /** Change the name of the object */
    def rename(name: String) = copy(name = name)
  }

  object Ref {
    /** A name-value pair for storing object fields */
    case class Field(value: RefTree, name: Option[String] = None, elideRefs: Boolean = false) {
      /** Add a name */
      def withName(name: String) = copy(name = Some(name))

      /** Remove the name */
      def withoutName = copy(name = None)

      /**
       * If this field points to a tree of type [[RefTree.Ref]],
       * configure whether that tree should be elided. It will not be visualized,
       * unless referenced by some other tree. This setting has no effects on other types of trees.
       */
      def withElideRefs(elideRefs: Boolean) = copy(elideRefs = elideRefs)

      /** Configure whether the tree this field points to should be highlighted */
      def withTreeHighlight(highlight: Boolean) = copy(value = value.withHighlight(highlight))
    }

    /**
     * Construct a [[RefTree]] for an object
     *
     * The identifier of the tree will be derived automatically.
     * Only modify it (e.g. via `copy`) if you know what you are doing ;)
     */
    def apply(value: AnyRef, children: Seq[Ref.Field]): Ref = Ref(
      // getSimpleName sometimes does not work, see https://issues.scala-lang.org/browse/SI-5425
      try { value.getClass.getSimpleName }
      catch { case _: InternalError ⇒ value.getClass.getName.replaceAll("^.+\\$", "") },
      s"${value.getClass.getName}${System.identityHashCode(value)}",
      children,
      highlight = false
    )
  }
}

/**
 * A typeclass for mapping data to [[RefTree]] representations
 */
@implicitNotFound("To render a diagram for type ${A}, implement an instance of reftree.core.ToRefTree[${A}]")
trait ToRefTree[A] { self ⇒
  def refTree(value: A): RefTree
}

object ToRefTree extends CollectionInstances with GenericInstances {
  /** A shorthand method for creating [[ToRefTree]] instances */
  def apply[A](toRefTree: A ⇒ RefTree): ToRefTree[A] = new ToRefTree[A] {
    def refTree(value: A) = toRefTree(value)
  }

  implicit def `AnyVal RefTree`[A <: AnyVal]: ToRefTree[A] = ToRefTree[A](RefTree.Val.apply)

  implicit def `String RefTree`: ToRefTree[String] = ToRefTree[String] { value ⇒
    RefTree.Ref(value, value.map(RefTree.Val(_).toField))
  }
}
