# Guide

## Trees

This library renders diagrams based on a simple data representation called
[`RefTree`](https://github.com/stanch/reftree/blob/master/core/src/main/scala/reftree/core/RefTree.scala).
Essentially, a `RefTree` denotes either an object (`AnyRef`) with a number of fields,
or a primitive (`AnyVal`).

To render a value of type `A`, you will need an implicit instance of `ToRefTree[A]`.
For many Scala collections, as well as case classes, no extra work is needed,
as these instances are readily available or generated on the fly.

You can configure the automatically generated instances like so:

```mdoc:silent
import reftree.core.ToRefTree

case class Tree(size: Int, value: Int, children: List[Tree])

implicit val treeDerivationConfig = (ToRefTree.DerivationConfig[Tree]
  .rename("MyTree")                                // display as “MyTree”
  .tweakField("size", _.withName("s"))             // label the field “s”, instead of “size”
  .tweakField("value", _.withTreeHighlight(true))  // highlight the value
  .tweakField("children", _.withoutName))          // do not label the “children” field

implicitly[ToRefTree[Tree]] // auto-derivation will use the configuration above
```

For something custom, manual derivation is the way to go, for example:

```mdoc:silent
import reftree.core._

implicit def treeInstance: ToRefTree[Tree] = ToRefTree[Tree] { tree =>
  RefTree.Ref(tree, Seq(
    // display the size as a hexadecimal number (why not?)
    RefTree.Val.formatted(tree.size)(_.toHexString).toField.withName("s"),
    // highlight the value
    tree.value.refTree.withHighlight(true).toField.withName("value"),
    // do not label the children
    tree.children.refTree.toField
  )).rename("MyTree") // change the name displayed for the class
}
```

## Renderers

To render diagrams and animations, you will need a `Renderer`.

**For JVM:**

```mdoc:invisible
val ImagePath = "site/target/tut/images"
```

```mdoc:silent
import reftree.render._
import reftree.diagram._
import java.nio.file.Paths

val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 75),
  directory = Paths.get(ImagePath, "guide")
)
```

You can also pass a `format` parameter as a String to the `Renderer` constructor
to specify the format you require. The default is `png`. You can specify any
file type supported by `dot -T`.

**For Scala.js:**

```scala
import reftree.render._
import reftree.diagram._

val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 75)
)
```

There are two ways to use renderers:

**JVM**

```mdoc:silent
import scala.collection.immutable.Queue

// Option 1: using the `render` method
renderer.render("queue", Diagram(Queue(1)))

// Option 2: using syntactic sugar
import renderer._
Diagram(Queue(1)).render("queue")
```

**Scala.js**

```scala
import org.scalajs.dom

// Option 1: using the `render` method
renderer.render(dom.document.getElementById("diagram"), Diagram(List(1)))

// Option 2: using syntactic sugar
import renderer._
Diagram(List(1)).render(dom.document.getElementById("diagram"))
```

You can set various options, for example:

```mdoc:silent
// using the `render` method
renderer.tweakRendering(_.withVerticalSpacing(2)).render("queue", Diagram(Queue(1)))

// using syntactic sugar
Diagram(Queue(1)).render("queue", _.withVerticalSpacing(2))
```

## Diagrams

Diagrams can be created and combined into bigger diagrams using the following API:

```mdoc:silent
// no caption
Diagram(Queue(1)).render("caption-none")
```

![caption-none](images/guide/caption-none.png)

```mdoc:silent
// automatically set caption to "Queue(1) :+ 2"
Diagram.sourceCodeCaption(Queue(1) :+ 2).render("caption-source")
```

![caption-source](images/guide/caption-source.png)

```mdoc:silent
// use toString to get the caption, i.e. "Queue(1, 2)"
Diagram.toStringCaption(Queue(1) :+ 2).render("caption-tostring")
```

![caption-tostring](images/guide/caption-tostring.png)

```mdoc:silent
// merge two diagrams, set captions manually
(Diagram(Queue(1)).withCaption("one") + Diagram(Queue(2)).withCaption("two")).render("one-two")
```

![one-two](images/guide/one-two.png)

```mdoc:silent
// isolate each diagram in its own namespace (graph nodes will not be shared across them)
(Diagram(Queue(1)).toNamespace("one") + Diagram(Queue(2)).toNamespace("two")).render("namespaced")
```

![namespaced](images/guide/namespaced.png)

## Animations

Animation is essentially a sequence of diagrams, which can be rendered to an animated GIF.
The simplest way to create an animation is to use the builder API:

```mdoc:silent
(Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) => queue :+ (i + 1))
  .build()
  .render("animation-simple"))
```

![animation-simple](images/guide/animation-simple.gif)

You can also configure how the diagram for each frame is produced:

```mdoc:silent
(Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) => queue :+ (i + 1))
  .build(Diagram(_).withCaption("My Queue").withColor(2))
  .render("animation-captioned-red"))
```

![animation-captioned-red](images/guide/animation-captioned-red.gif)

Note that by default the library will try to reduce the average movement of
all tree nodes across animation frames. Sometimes you want to “anchor”
the root of the data structure instead, to force it to stay still
while everything else is moving. You can achieve this via `withAnchor` method:

```mdoc:silent
(Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) => queue :+ (i + 1))
  .build(Diagram(_).withAnchor("queue").withCaption("This node is anchored!"))
  .render("animation-anchored"))
```

![animation-anchored](images/guide/animation-anchored.gif)

Finally, animations can be combined in sequence or in parallel, for example:

```mdoc:silent
val queue1 = (Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) => queue :+ (i + 1))
  .build()
  .toNamespace("one"))

val queue2 = (Animation
  .startWith(Queue(10))
  .iterateWithIndex(2)((queue, i) => queue :+ (10 * (i + 1)))
  .build()
  .toNamespace("two"))

(queue1 + queue2).render("animation-parallel")
```

![animation-parallel](images/guide/animation-parallel.gif)
