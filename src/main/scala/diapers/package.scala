package object diapers {
  implicit class RefTreeSyntax[A: ToRefTree](value: A) {
    def refTree = implicitly[ToRefTree[A]].refTree(value)
  }
}
