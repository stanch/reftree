package reftree.diagram

import reftree.core._

/**
 * The central type for producing animations — sequences of diagrams
 *
 * Animations can be combined in parallel (using `+`) or in sequence (using `chain`).
 *
 * While in most cases this library provides sane defaults,
 * various techniques can be applied to ensure continuity between the frames:
 *
 *   - By default, the adjacent frames are aligned to reduce the overall movement
 *     of the tree nodes inside them. You can “anchor” the roots of one or more diagrams
 *     to minimize their movement instead, while allowing everything else to move (in case
 *     of several anchors in one frame, completely pinning all of them might not be possible).
 *     To do that, use the `withAnchor` method for each diagram in question, and provide
 *     the same anchor id for the same diagram across different frames.
 *
 *   - By default, colors are auto-assigned within each frame. However if you have
 *     several diagrams in each frame and their number or order changes, you will want
 *     to assign colors manually. You can do that by using the `withColor` diagram method
 *     and providing the same color palette index for the same diagram across different frames.
 *
 *   - When combining several animations in parallel, you might want to prevent the trees
 *     inside them from sharing nodes with the same ids. This can be achieved with the
 *     namespacing functionality (see [[Diagram]] for more details). Just like with diagrams,
 *     you can use the `toNamespace` method to adjust that for the entire animation.
 *
 * Usage examples:
 * {{{
 *   // Basic animation
 *   Animation
 *     .startWith(Queue(1))
 *     .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
 *     .build()
 *
 *   // Configure how the diagram for each frame is produced
 *   Animation
 *     .startWith(Queue(1))
 *     .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
 *     .build(Diagram(_).withCaption("My Queue").withColor(2))
 *
 *   // Adding anchors
 *   Animation
 *     .startWith(Queue(1))
 *     .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
 *     .build(Diagram(_).withAnchor("queue").withCaption("This node is anchored!"))
 *
 *   // Combining in parallel
 *   Animation
 *     .startWith(Queue(1))
 *     .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
 *     .build()
 *     .toNamespace("one") +
 *   Animation
 *     .startWith(Queue(10))
 *     .iterateWithIndex(2)((queue, i) ⇒ queue :+ (10 * (i + 1)))
 *     .build()
 *     .toNamespace("two")
 * }}}
 */
case class Animation(diagrams: Seq[Diagram]) {
  /** Combine with another animation in parallel */
  def +(that: Animation) = Animation {
    diagrams.zipAll(that.diagrams, Diagram.empty, Diagram.empty).map { case (a, b) ⇒ a + b }
  }

  /** Combine with another animation in sequence */
  def chain(that: Animation) = Animation(this.diagrams ++ that.diagrams)

  /** Chain with the reverse of this animation for the extra wow factor */
  def mirror = Animation(diagrams ++ diagrams.reverse)

  /** Put this animation into a namespace with the given name (add one more hierarchy level) */
  def toNamespace(name: String) = copy(diagrams = diagrams.map(_.toNamespace(name)))
}

object Animation {
  /** Create an animation builder from a starting value */
  def startWith[A: ToRefTree](start: A) = Builder(Vector(start))

  /** Create an animation builder from a sequence of starting values */
  def startWithSequence[A: ToRefTree](start: Seq[A]) = Builder(start.toVector)

  /** A builder for animations */
  case class Builder[A: ToRefTree](frames: Vector[A]) {
    /** Add more frames by applying the provided iteration functions */
    def iterate(iterations: (A ⇒ A)*) =
      Builder(iterations.foldLeft(frames)((current, step) ⇒ current :+ step(current.last)))

    /** Add more frames by applying the provided iteration function `n` times */
    def iterate(n: Int)(iteration: A ⇒ A) =
      Builder((1 to n).foldLeft(frames)((current, _) ⇒ current :+ iteration(current.last)))

    /** Add more frames by applying the provided iteration function `n` times */
    def iterateWithIndex(n: Int)(iteration: (A, Int) ⇒ A) =
      Builder((1 to n).foldLeft(frames)((current, i) ⇒ current :+ iteration(current.last, i)))

    /** Iterate on the builder itself */
    def repeat(operations: (Builder[A] ⇒ Builder[A])*) =
      operations.foldLeft(this)((current, operation) ⇒ operation(current))

    /** Iterate on the builder itself `n` times */
    def repeat(n: Int)(operation: Builder[A] ⇒ Builder[A]) =
      (1 to n).foldLeft(this)((current, _) ⇒ operation(current))

    /** Iterate on the builder itself `n` times */
    def repeatWithIndex(n: Int)(operation: (Builder[A], Int) ⇒ Builder[A]) =
      (1 to n).foldLeft(this)((current, i) ⇒ operation(current, i))

    /** Build an animation, optionally specifying how to construct diagrams from data */
    def build(diagram: A ⇒ Diagram = value ⇒ Diagram.toStringCaption(value)) =
      Animation(frames.map(diagram))

    /** Build an animation, optionally specifying how to construct diagrams from data and frame numbers */
    def buildWithIndex(diagram: (A, Int) ⇒ Diagram = (value, _) ⇒ Diagram.toStringCaption(value)) =
      Animation(frames.zipWithIndex.map(diagram.tupled))
  }
}
