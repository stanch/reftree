package reftree.diagram

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class AnimationSpec extends FlatSpec with PropertyChecks with Matchers {

  "iterateUntilFixPoint" should "expand until fixpoint" in {

    forAll { seed: Int =>
      whenever(seed > 0) {
        val fixPointBuilder =
          Animation.startWith(seed).iterateToFixpoint(_ / 2)
        val expectedSize = (math.log(seed) / math.log(2)) + 2 //first + last frame
        fixPointBuilder.frames should have size expectedSize.toInt
      }
    }

  }

  "iterateUntilFixPointAtMost" should "expand until fixpoint or max" in {

    forAll(Gen.posNum[Int]) { seed: Int =>
      whenever(seed > 0) {
        val max = 5
        val fixPointBuilder =
          Animation.startWith(seed).iterateUntilFixpointAtMost(max)(_ / 2)
        val expectedSize = math.min(max, (math.log(seed) / math.log(2)) + 2) //first + last frame
        fixPointBuilder.frames should have size expectedSize.toInt
      }
    }

  }

  "iterateUntil" should "expand until some predicate is true" in {

    val fixPointBuilder = Animation.startWith(0).iterateTo(_ > 100)(_ + 13)
    fixPointBuilder.frames shouldEqual Vector(0, 13, 26, 39, 52, 65, 78, 91,
      104)

  }

  "iterateUntilAtMost" should "expand until some predicate is true or max" in {

    val max = 5
    val fixPointBuilder =
      Animation.startWith(0).iterateToAtMost(max)(_ > 100)(_ + 13)
    fixPointBuilder.frames shouldEqual Vector(0, 13, 26, 39, 52)

    val fixPointSmall =
      Animation.startWith(0).iterateToAtMost(max)(_ > 30)(_ + 13)
    fixPointSmall.frames shouldEqual Vector(0, 13, 26, 39)

  }
}
