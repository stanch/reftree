package reftree.contrib

import reftree._
import zipper.Zipper

object ZipperInstances {
  implicit def elideParent[A](implicit default: ToRefTree[Zipper[A]]): ToRefTree[Zipper[A]] =
    default.suppressField(3)
}
