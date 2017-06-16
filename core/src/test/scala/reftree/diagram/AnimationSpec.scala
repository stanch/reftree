package reftree.diagram

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class AnimationSpec extends FlatSpec with Matchers {

  "iterateUntilFixPoint" should "expand until fixpoint" in {

    val seed = 1337
    val maxTests = 1200

    (0 until 4200).foreach { _ =>
      val seed = math.abs(Random.nextInt()) + 1
      val fixPointBuilder =
        Animation.iterateUntilFixPoint(seed)(_ / 2)(max = Int.MaxValue)
      val expectedSize = (math.log(seed) / math.log(2)) + 2 //first + last frame

      fixPointBuilder.frames should have size expectedSize.toInt

    }

  }

  "iterateUntil" should "expand until some predicate is true" in {

    val fixPointBuilder = Animation.iterateUntil(0)(_ + 13)(_ > 100)(max = Int.MaxValue)
    fixPointBuilder.frames shouldEqual Vector(0, 13, 26, 39, 52, 65, 78, 91, 117)

  }
}
