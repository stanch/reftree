package reftree.dot

import scala.language.higherKinds

/**
 * A chunk obtained in the process of encoding
 */
case class Chunk(encoded: String) {
  /** If this chunk is non-empty, wrap it with a prefix and a suffix */
  def wrap(prefix: String, suffix: String) =
    if (encoded.nonEmpty) Chunk(prefix + encoded + suffix) else this
}

object Chunk {
  /** An empty chunk */
  val empty = Chunk("")

  /** Concatenate non-empty chunks */
  def join(chunks: Chunk*) =
    Chunk(chunks.map(_.encoded).filter(_.nonEmpty).mkString)

  /** Concatenate non-empty chunks with a delimiter */
  def join(delimiter: String)(chunks: Chunk*) =
    Chunk(chunks.map(_.encoded).filter(_.nonEmpty).mkString(delimiter))
}

/**
 * A typeclass for encoding values of type [[A]] into chunks
 */
trait Encoding[A] {
  def encoding: A ⇒ Chunk
}

/**
 * Common utilities for implementing encodings
 */
trait EncodingCompanion[R, E[X] <: Encoding[X]] {
  /** Convenient syntax */
  implicit class Syntax[A](value: A)(implicit encoding: E[A]) {
    def encoded: Chunk = encoding.encoding(value)
  }

  /** The root encoding */
  def root: E[R]

  /** The encoding entrypoint */
  def encode(value: R) = root.encoding(value).encoded
}
