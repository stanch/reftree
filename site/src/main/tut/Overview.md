# Overview

Behold, automatically generated diagrams and animations for your data!
`reftree` is a *Scala* and *Scala.js* library that allows you to
create data structure visualizations with very little effort.

![teaser](images/teaser.gif)

There are a few ways you can use `reftree`:

* [improving the documentation of your projects](https://stanch.github.io/zipper/);
* [live-coding demos and talks](Talks.md);
* exploring how things work;
* anywhere you need diagrams of your Scala data structures.

## Features

* Pre-made visualizations of many standard collections:
  [lists, queues, vectors, etc](talks/Immutability.html#immutable-data-structures).

  ![lists](images/immutability/data/lists.png)

* Automatic visualization of case classes (using
  [shapeless](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#generic-representation-of-sealed-families-of-case-classes)).

  ```scala
  case class Employee(
    name: String,
    salary: Long
  )

  case class Startup(
    name: String,
    founder: Employee,
    team: List[Employee]
  )
  ```

  ![startup](images/immutability/lenses/startup.png)

* Static diagrams as well as animations can be generated.
* Hassle-free captions (using [sourcecode](https://github.com/lihaoyi/sourcecode)).
* Scala.js support (*experimental*).

## Getting Started

To use this library you will need to have [GraphViz](http://www.graphviz.org/) installed (and have `dot` on your `PATH`).
I also recommend to install the [Source Code Pro](https://github.com/adobe-fonts/source-code-pro) fonts (regular and *italic*),
as I find they look the best among the free options and therefore are used by default.

For viewing PNG and animated GIF on Linux I recommend `eog` and `gifview` respectively.

### Interactive usage

To jump into an interactive session:

```
$ git clone https://github.com/stanch/reftree
$ cd reftree
$ sbt demo
@ render(List(1, 2, 3))
// display diagram.png with your favorite image viewer
```

### Including in your project

You can depend on the library by adding these lines to your `build.sbt`:

[ ![Download](https://api.bintray.com/packages/stanch/maven/reftree/images/download.svg) ](https://bintray.com/stanch/maven/reftree/_latestVersion)

```scala
// JVM
libraryDependencies += "org.stanch" %% "reftree" % "latest-version"

// Scala.js
libraryDependencies += "org.stanch" %%% "reftree" % "latest-version"
```

### Minimal example

```tut:invisible
val ImagePath = "site/target/tut/images"
```

```tut:silent
import reftree.render.{Renderer, RenderingOptions}
import reftree.diagram.Diagram
import java.nio.file.Paths

val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 75),
  directory = Paths.get(ImagePath, "overview")
)
import renderer._

case class Person(firstName: String, age: Int)

Diagram.sourceCodeCaption(Person("Bob", 42)).render("example")
```

![bob](images/overview/example.png)

For more details, please refer to the [guide](Guide.md).
