package reftree.contrib

import reftree.core._
import zipper.Zipper
import com.softwaremill.quicklens._

object ZipperInstances {
  implicit def zipper[A](implicit default: ToRefTree[Zipper[A]]): ToRefTree[Zipper[A]] =
    default.highlightField(1)

  case class ZipperFocus[A](zipper: Zipper[A], target: A)

  object ZipperFocus {
    def apply[A](zipper: Zipper[A]): ZipperFocus[A] = ZipperFocus(zipper, zipper.commit)
  }

  implicit def `ZipperFocus RefTree`[A](implicit refTreeA: ToRefTree[A]): ToRefTree[ZipperFocus[A]] =
    ToRefTree[ZipperFocus[A]] { value ⇒
      val focus = value.zipper.focus.refTree

      def inner(tree: RefTree): RefTree = tree match {
        case `focus` ⇒ tree.withHighlight(true)
        case ref: RefTree.Ref ⇒ ref.modify(_.children.each).using(inner)
        case _ ⇒ tree
      }

      inner(value.target.refTree)
    }
}
