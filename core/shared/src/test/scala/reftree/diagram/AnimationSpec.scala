package reftree.diagram

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class AnimationSpec extends FlatSpec with PropertyChecks with Matchers {
  it should "correctly iterate to fixpoint" in {
    forAll(Gen.posNum[Int]) { seed ⇒
      val builder = Animation.startWith(seed).iterate(_ + 1).iterateToFixpoint(_ / 2)
      val expectedSize = (math.log(seed + 1) / math.log(2)) + 3

      builder.frames should have size expectedSize.toInt
    }
  }

  it should "correctly iterate to fixpoint or max, when specified" in {
    forAll(Gen.posNum[Int]) { seed ⇒
      val max = 5
      val builder = Animation.startWith(seed).iterate(_ + 1).iterateToFixpointAtMost(max)(_ / 2)
      val expectedSize = math.min(max + 2, (math.log(seed + 1) / math.log(2)) + 3)

      builder.frames should have size expectedSize.toInt
    }
  }

  it should "correctly iterate while some predicate is true" in {
    val builder = Animation.startWith(-1).iterate(_ + 1).iterateWhile(_ < 100)(_ + 13)
    builder.frames shouldEqual Vector(-1, 0, 13, 26, 39, 52, 65, 78, 91)
  }

  it should "correctly iterate while some predicate is true or max is reached" in {
    val max = 5

    val builder1 = Animation.startWith(-1).iterate(_ + 1).iterateWhileAtMost(max)(_ < 100)(_ + 13)
    builder1.frames shouldEqual Vector(-1, 0, 13, 26, 39, 52, 65)

    val builder2 = Animation.startWith(-1).iterate(_ + 1).iterateWhileAtMost(max)(_ < 30)(_ + 13)
    builder2.frames shouldEqual Vector(-1, 0, 13, 26)
  }
}
