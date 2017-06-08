package scala.collection.immutable

/**
 * [[ToRefTree]] instances for Scala immutable collections, which require access to private fields
 *
 * This does not seem to be feasible in Scala.js.
 *
 * The package name is intentionally changed so that we can get access to some private fields and classes.
 */
trait HackedCollectionInstances
