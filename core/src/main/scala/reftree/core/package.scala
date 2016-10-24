package reftree

package object core {
  implicit class RefTreeSyntax[A: core.ToRefTree](value: A) {
    def refTree = implicitly[core.ToRefTree[A]].refTree(value)
  }
}
