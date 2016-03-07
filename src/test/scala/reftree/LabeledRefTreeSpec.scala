package reftree

import org.scalatest.{Matchers, FlatSpec}

class LabeledRefTreeSpec extends FlatSpec with Matchers {
  it should "derive tree labels automatically" in {
    def x(l: LabeledRefTree) = l.label
    x(List(1, 2, 3)) shouldEqual "List(1, 2, 3)"
  }
}
