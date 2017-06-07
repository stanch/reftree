package reftree.dot

import scala.language.higherKinds

/**
 * A chunk obtained in the process of encoding a value of type [[Type]]
 */
case class Chunk[Type](encoded: String) {
  /** If this chunk is non-empty, wrap it with a prefix and a suffix */
  def wrap(prefix: String, suffix: String) =
    if (encoded.nonEmpty) Chunk[Type](prefix + encoded + suffix) else this
}

object Chunk {
  /** An empty chunk */
  def empty[Type] = Chunk[Type]("")

  /** Concatenate non-empty chunks */
  def join[Type](chunks: Chunk[Type]*) =
    Chunk[Type](chunks.map(_.encoded).filter(_.nonEmpty).mkString)

  /** Concatenate non-empty chunks with a delimiter */
  def join[Type](delimiter: String)(chunks: Chunk[Type]*) =
    Chunk[Type](chunks.map(_.encoded).filter(_.nonEmpty).mkString(delimiter))
}

/**
 * A typeclass for encoding values of type [[A]] into chunks, for some root type [[Type]]
 */
trait Encoding[Type, A] {
  def encoding: A ⇒ Chunk[Type]
}

/**
 * Common utilities for implementing encodings
 */
trait EncodingCompanion[Type, E[X] <: Encoding[Type, X]] {
  /** Convenient syntax */
  implicit class Syntax[A](value: A)(implicit encoding: E[A]) {
    def encoded: Chunk[Type] = encoding.encoding(value)
  }

  /** A chunk with raw, unencoded data */
  def raw(content: String) = Chunk[Type](content)

  /** A method for encoding the root type [[Type]] */
  def root: E[Type]

  /** The encoding entrypoint */
  def encode(value: Type) = root.encoding(value).encoded
}
