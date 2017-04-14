package reftree.util

import monocle._
import monocle.function.all.each
import monocle.std.list.listEach
import zipper.{Unzip, Zipper}

import scala.collection.immutable.ListMap

object Optics {
  /** Tuple two lenses with a common source type */
  def tupleLensLeft[S, A, B](lensA: Lens[S, A], lensB: Lens[S, B]): Lens[S, (A, B)] =
    Lens[S, (A, B)](s ⇒ (lensA.get(s), lensB.get(s))) {
      case (a, b) ⇒ s ⇒ lensB.set(b)(lensA.set(a)(s))
    }

  /** Tuple three lenses with a common source type */
  def tupleLensLeft[S, A, B, C](lensA: Lens[S, A], lensB: Lens[S, B], lensC: Lens[S, C]): Lens[S, (A, B, C)] =
    Lens[S, (A, B, C)](s ⇒ (lensA.get(s), lensB.get(s), lensC.get(s))) {
      case (a, b, c) ⇒ s ⇒ lensC.set(c)(lensB.set(b)(lensA.set(a)(s)))
    }

  /** A view into the subset of elements of a sequence satisfying a predicate */
  def partitionLens[A](pred: A ⇒ Boolean): Lens[Seq[A], Seq[A]] =
    Lens[Seq[A], Seq[A]](_.filter(pred)) { updated ⇒ sequence ⇒
      sequence.foldLeft((Vector.empty[A], updated)) {
        case ((acc, u), current) ⇒
          if (pred(current)) (acc :+ u.head, u.tail)
          else (acc :+ current, u)
      }._1
    }

  /** Transforms a lens into a lens of lists */
  def sequenceLens[A, B](lens: Lens[A, B]): Lens[List[A], List[B]] =
    Lens[List[A], List[B]] { as ⇒
      as.map(lens.get)
    } { bs ⇒ as ⇒
      (bs zip as).map { case (b, a) ⇒ lens.set(b)(a) }
    }

  /** A prism that matches values satisfying a predicate */
  def only[A](pred: A ⇒ Boolean): Prism[A, A] =
    Prism[A, A] { value ⇒
      if (pred(value)) Some(value) else None
    }(identity)

  /**
   * Given a recursive data structure and a move to traverse it with a zipper,
   * focuses on the first element within that structure that satisfies a predicate.
   */
  private def collectOne[A: Unzip](pred: A ⇒ Boolean, move: Zipper.Move[A]): Optional[A, A] = {
    Optional[A, A] { value ⇒
      Zipper(value).tryRepeatWhileNot(pred, move).toOption.map(_.focus)
    } { collected ⇒ value ⇒
      Zipper(value).tryRepeatWhileNot(pred, move).toOption.fold(value)(_.set(collected).commit)
    }
  }

  /**
   * Focuses on the first element of a recursive structure that satisfies a predicate
   * (traversal order is depth-first, left-to-right).
   */
  def collectFirst[A: Unzip](pred: A ⇒ Boolean): Optional[A, A] =
    collectOne(pred, _.tryAdvanceRightDepthFirst)

  /**
   * Focuses on the last element of a recursive structure that satisfies a predicate
   * (traversal order is depth-first, left-to-right).
   */
  def collectLast[A: Unzip](pred: A ⇒ Boolean): Optional[A, A] =
    collectOne(pred, _.tryAdvanceLeftDepthFirst)

  /**
   * Focuses on all elements of a recursive structure that satisfy a predicate,
   * where each element is pointed to by a key obtained via a key function.
   *
   * The elements are arranged in the resulting map in order of appearance in the structure
   * (traversed depth-first, left-to-right).
   *
   * On update, elements with missing keys will be deleted, and elements with new keys
   * will be inserted at the root of the structure.
   */
  def collectLeftByKey[A: Unzip, B](pred: A ⇒ Boolean)(key: A ⇒ B): Lens[A, ListMap[B, A]] = {
    Lens[A, ListMap[B, A]] {
      Zipper(_).loopAccum(ListMap.empty[B, A]) { (z, m) ⇒
        if (pred(z.focus)) (z.tryAdvanceRightDepthFirst, m.updated(key(z.focus), z.focus))
        else (z.tryAdvanceRightDepthFirst, m)
      }._2
    } { children ⇒ svg ⇒
      val (zipper, remaining) = Zipper(svg).loopAccum(children) { (z, m) ⇒
        if (!pred(z.focus)) (z.tryAdvanceRightDepthFirst, m) else {
          val i = key(z.focus)
          if (m contains i) (z.set(m(i)).tryAdvanceRightDepthFirst, m - i)
          else (z.tryDeleteAndAdvanceRightDepthFirst, m)
        }
      }
      zipper.cycle(_.tryMoveUp).insertDownRight(remaining.values.toList).commit
    }
  }

  /**
   * Focuses on all elements of a recursive structure that satisfy a predicate,
   * in order of appearance in the structure (traversed depth-first, left-to-right).
   *
   * This lens assumes that the number of elements is preserved on update.
   */
  def collectLeftByIndex[A: Unzip](pred: A ⇒ Boolean): Lens[A, List[A]] = {
    Lens[A, List[A]] {
      Zipper(_).loopAccum(List.empty[A]) { (z, s) ⇒
        if (pred(z.focus)) (z.tryAdvanceRightDepthFirst, z.focus :: s)
        else (z.tryAdvanceRightDepthFirst, s)
      }._2.reverse
    } { children ⇒
      Zipper(_).loopAccum(children) { (z, s) ⇒
        if (pred(z.focus)) (z.set(s.head).tryAdvanceRightDepthFirst, s.tail)
        else (z.tryAdvanceRightDepthFirst, s)
      }._1.commit
    }
  }

  /**
   * Focuses on all elements of a recursive structure that satisfy a predicate,
   * in order of appearance in the structure (traversed depth-first, left-to-right).
   */
  def collectAllLeft[A: Unzip](pred: A ⇒ Boolean): Traversal[A, A] =
    collectLeftByIndex(pred) composeTraversal each

  /** Focuses on a given optional attribute of an XML node */
  def xmlAttribute(attr: String): Lens[xml.Node, Option[String]] =
    Lens[xml.Node, Option[String]] { node ⇒
      node.attribute(attr).map(_.text)
    } { value ⇒ node ⇒
      node.asInstanceOf[xml.Elem].copy(
        attributes = value.fold(node.attributes.remove(attr)) { v ⇒
          node.attributes append new xml.UnprefixedAttribute(attr, v, xml.Null)
        }
      )
    }

  /** Focuses on a given mandatory attribute of an XML node */
  def xmlMandatoryAttribute(attr: String): Lens[xml.Node, String] =
    Lens[xml.Node, String] { node ⇒
      node.attribute(attr).map(_.text).get
    } { value ⇒ node ⇒
      node.asInstanceOf[xml.Elem].copy(
        attributes = node.attributes append new xml.UnprefixedAttribute(attr, value, xml.Null)
      )
    }

  /** Focuses on a given optional attribute of an XML node */
  def xmlPrefixedAttribute(uri: String, attr: String): Lens[xml.Node, Option[String]] =
    Lens[xml.Node, Option[String]] { svg ⇒
      svg.attribute(uri, attr).map(_.text)
    } { value ⇒ svg ⇒
      svg.asInstanceOf[xml.Elem].copy(
        // TODO: how to remove a prefixed attribute?
        attributes = value.fold(svg.attributes.remove(attr)) { v ⇒
          svg.attributes append new xml.PrefixedAttribute(uri, attr, v, xml.Null)
        }
      )
    }

  /** Focuses on the immediate children of an XML node, if any */
  def xmlImmediateChildren: Optional[xml.Node, List[xml.Node]] =
    Optional[xml.Node, List[xml.Node]] {
      case xml.Elem(_, _, _, _, children @ _*) if children.nonEmpty ⇒ Some(children.toList)
      case _ ⇒ None
    } (children ⇒ {
      case elem: xml.Elem ⇒ elem.copy(child = children)
      case other ⇒ other
    })

  /**
   * Derive a [[zipper.Unzip]] instance for a type `A`, given an [[Optional]]
   * that extracts immediate children of a node of type `A`
   */
  def unzip[A](immediateChildren: Optional[A, List[A]]): Unzip[A] = new Unzip[A] {
    def unzip(node: A): List[A] =
      immediateChildren.getOption(node).getOrElse(List.empty)

    def zip(node: A, children: List[A]): A =
      immediateChildren.set(children)(node)
  }
}
