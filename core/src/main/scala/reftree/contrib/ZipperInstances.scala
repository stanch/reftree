package reftree.contrib

import reftree._
import zipper.Zipper
import com.softwaremill.quicklens._

object ZipperInstances {
  case class ZipperFocus[A](zipper: Zipper[A], target: A)

  object ZipperFocus {
    def apply[A](zipper: Zipper[A]): ZipperFocus[A] = ZipperFocus(zipper, zipper.commit)
  }

  implicit def `ZipperFocus RefTree`[A](implicit refTreeA: ToRefTree[A]): ToRefTree[ZipperFocus[A]] =
    new ToRefTree[ZipperFocus[A]] {
      def refTree(value: ZipperFocus[A]): RefTree = {
        val focus = value.zipper.focus.refTree

        def inner(tree: RefTree): RefTree = tree match {
          case `focus` ⇒ tree.modify(_.highlight).setTo(true)
          case ref: RefTree.Ref ⇒ ref.modify(_.children.each).using(inner)
          case _ ⇒ tree
        }

        inner(value.target.refTree)
      }
    }
}
