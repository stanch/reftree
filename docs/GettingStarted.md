---
sidebar_position: 2
---

# Getting started

To use this library you will need to have [GraphViz](http://www.graphviz.org/) installed (and have `dot` on your `PATH`).
I also recommend to install the [Source Code Pro](https://github.com/adobe-fonts/source-code-pro) fonts (regular and *italic*),
as I find they look the best among the free options and therefore are used by default.

For viewing PNG and animated GIF on Linux I recommend `eog` and `gifview` respectively.

## Interactive usage

To jump into an interactive session:

```
$ git clone https://github.com/stanch/reftree
$ cd reftree
$ sbt demo
@ render(List(1, 2, 3))
// display diagram.png with your favorite image viewer
```

## Including in your project

`reftree` is available for Scala 2.12 and 2.13. You can depend on the library by adding these lines to your `build.sbt`:

```mdx-code-block
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
```

<Tabs groupId="platform">
  <TabItem value="jvm" label="JVM" default>

```scala
libraryDependencies += "io.github.stanch" %% "reftree" % "@VERSION@"
```

  </TabItem>
  <TabItem value="js" label="Scala.js">

```scala
libraryDependencies += "io.github.stanch" %%% "reftree" % "@VERSION@"
```

  </TabItem>
</Tabs>

## Minimal example

```scala mdoc:invisible
val ImagePath = "site-gen/target/mdoc/images"
```

```scala mdoc:silent
import reftree.render.{Renderer, RenderingOptions}
import reftree.diagram.Diagram
import java.nio.file.Paths
```

```scala
val ImagePath = "images"
```

```scala mdoc:silent
val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 100),
  directory = Paths.get(ImagePath, "overview")
)
import renderer._

case class Person(firstName: String, age: Int)

Diagram.sourceCodeCaption(Person("Bob", 42)).render("example")
```

This generates `images/overview/example.png` with the following image:

![bob](images/overview/example.png)

For more details, please refer to the [guide](Guide.md).
