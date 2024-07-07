package reftree.util

import monocle._
import monocle.function.all.each
import zipper.{Unzip, Zipper}

import scala.collection.immutable.ListMap

object Optics {
  /** Tuple two optionals with a common source type */
  def tupleOptionalLeft[S, A, B](
    optionalA: Optional[S, A],
    optionalB: Optional[S, B]
  ): Optional[S, (A, B)] =
    Optional[S, (A, B)] { s =>
      for {
        a <- optionalA.getOption(s)
        b <- optionalB.getOption(s)
      } yield (a, b)
    } {
      case (a, b) => s => optionalB.set(b)(optionalA.set(a)(s))
    }

  /** Tuple three optionals with a common source type */
  def tupleOptionalLeft[S, A, B, C](
    optionalA: Optional[S, A],
    optionalB: Optional[S, B],
    optionalC: Optional[S, C]
  ): Optional[S, (A, B, C)] =
    Optional[S, (A, B, C)] { s =>
      for {
        a <- optionalA.getOption(s)
        b <- optionalB.getOption(s)
        c <- optionalC.getOption(s)
      } yield (a, b, c)
    } {
      case (a, b, c) => s => optionalC.set(c)(optionalB.set(b)(optionalA.set(a)(s)))
    }

  /** A prism that matches values satisfying a predicate */
  def only[A](pred: A => Boolean): Prism[A, A] =
    Prism[A, A] { value =>
      if (pred(value)) Some(value) else None
    }(identity)

  /** Convenience methods for Optionals */
  implicit class RichOptional[A, B](val optional: Optional[A, B]) extends AnyVal {
    def setOption(inner: Option[B])(outer: A) =
      inner.fold(outer)(optional.set(_)(outer))

    def asOptionGetter =
      Getter[A, Option[B]](optional.getOption)
  }

  /** Extra flattening for Lenses that point to optional values */
  implicit class RichLens[A, B](val lens: Lens[A, Option[B]]) extends AnyVal {
    def asFlatOptional = Optional[A, B](lens.get)(b => lens.set(Some(b)))
  }

  /**
   * Given a recursive data structure and a move to traverse it with a zipper,
   * focuses on the projection of the first (projectable) element within that structure.
   */
  private def collectOne[A: Unzip, B](projection: Optional[A, B], move: Zipper.Move[A]): Optional[A, B] = {
    Optional[A, B] { value =>
      Zipper(value).tryRepeatWhileNot(projection.nonEmpty, move)
        .toOption.map(_.focus).flatMap(projection.getOption)
    } { collected => value =>
      Zipper(value).tryRepeatWhileNot(projection.nonEmpty, move)
        .toOption.fold(value)(_.update(projection.set(collected)).commit)
    }
  }

  /**
   * Focuses on the projection of the first (projectable) element of a recursive structure
   * (traversal order is depth-first, left-to-right).
   */
  def collectFirst[A: Unzip, B](projection: Optional[A, B]): Optional[A, B] =
    collectOne(projection, _.tryAdvanceRightDepthFirst)

  /**
   * Focuses on the projection of the first (projectable) element of a recursive structure
   * (traversal order is depth-first, left-to-right).
   */
  def collectFirst[A: Unzip, B](projection: Prism[A, B]): Optional[A, B] =
    collectFirst(projection.asOptional)

  /**
   * Focuses on the projection of the last (projectable) element of a recursive structure
   * (traversal order is depth-first, left-to-right).
   */
  def collectLast[A: Unzip, B](projection: Optional[A, B]): Optional[A, B] =
    collectOne(projection, _.tryAdvanceLeftDepthFirst)

  /**
   * Focuses on the projection of the last (projectable) element of a recursive structure
   * (traversal order is depth-first, left-to-right).
   */
  def collectLast[A: Unzip, B](projection: Prism[A, B]): Optional[A, B] =
    collectLast(projection.asOptional)

  /**
   * Focuses on projections of all (projectable) elements of a recursive structure,
   * where each projection is pointed to by a key, obtained from the element via a key function.
   *
   * The projections are arranged in the resulting map in order of appearance in the structure
   * (traversed depth-first, left-to-right).
   *
   * On update, elements with missing keys will be deleted, and projections with new keys
   * will be inserted at the root of the structure.
   */
  def collectLeftByKey[A: Unzip, K, V](projection: Prism[A, V])(key: A => K): Lens[A, ListMap[K, V]] = {
    Lens[A, ListMap[K, V]] {
      Zipper(_).loopAccum(ListMap.empty[K, V]) { (z, m) =>
        projection.getOption(z.focus) match {
          case Some(focus) => (z.tryAdvanceRightDepthFirst, m.updated(key(z.focus), focus))
          case None => (z.tryAdvanceRightDepthFirst, m)
        }
      }._2
    } { children => node =>
      val (zipper, remaining) = Zipper(node).loopAccum(children) { (z, m) =>
        projection.getOption(z.focus) match {
          case Some(focus) =>
            val i = key(z.focus)
            if (m contains i) (z.set(projection.reverseGet(m(i))).tryAdvanceRightDepthFirst, m - i)
            else (z.tryDeleteAndAdvanceRightDepthFirst, m)
          case None =>
            (z.tryAdvanceRightDepthFirst, m)
        }
      }
      zipper.cycle(_.tryMoveUp).insertDownRight(remaining.values.toList.map(projection.reverseGet)).commit
    }
  }

  /**
   * Focuses on projections of all (projectable) elements of a recursive structure,
   * in order of appearance in the structure (traversed depth-first, left-to-right).
   *
   * This lens assumes that the number of elements is preserved on update.
   */
  def collectLeftByIndex[A: Unzip, B](projection: Optional[A, B]): Lens[A, List[B]] = {
    Lens[A, List[B]] {
      Zipper(_).loopAccum(List.empty[B]) { (z, s) =>
        projection.getOption(z.focus) match {
          case Some(focus) => (z.tryAdvanceRightDepthFirst, focus :: s)
          case None => (z.tryAdvanceRightDepthFirst, s)
        }
      }._2.reverse
    } { children =>
      Zipper(_).loopAccum(children) { (z, s) =>
        projection.getOption(z.focus) match {
          case Some(focus) => (z.update(projection.set(s.head)).tryAdvanceRightDepthFirst, s.tail)
          case None => (z.tryAdvanceRightDepthFirst, s)
        }
      }._1.commit
    }
  }

  /**
   * Focuses on projections of all (projectable) elements of a recursive structure,
   * in order of appearance in the structure (traversed depth-first, left-to-right).
   *
   * This lens assumes that the number of elements is preserved on update.
   */
  def collectLeftByIndex[A: Unzip, B](projection: Prism[A, B]): Lens[A, List[B]] =
    collectLeftByIndex(projection.asOptional)

  /**
   * Focuses on projections of all (projectable) elements of a recursive structure,
   * in order of appearance in the structure (traversed depth-first, left-to-right).
   */
  def collectAllLeft[A: Unzip, B](projection: Optional[A, B]): Traversal[A, B] =
    collectLeftByIndex(projection) composeTraversal each

  /**
   * Focuses on projections of all (projectable) elements of a recursive structure,
   * in order of appearance in the structure (traversed depth-first, left-to-right).
   */
  def collectAllLeft[A: Unzip, B](projection: Prism[A, B]): Traversal[A, B] =
    collectAllLeft(projection.asOptional)



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
