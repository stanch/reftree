package reftree.render

import java.nio.file.{Paths, Path}

import reftree.diagram.{Animation, Diagram}
import reftree.graph.Graphs
import reftree.svg.SvgGraphAnimation

case class Renderer(
  renderingOptions: RenderingOptions = RenderingOptions(),
  animationOptions: AnimationOptions = AnimationOptions(),
  directory: Path = Paths.get(".")
) { self ⇒
  def render(name: String, diagram: Diagram): Unit = {
    val graph = Graphs.graph(renderingOptions)(diagram)
    PngRenderer.renderPng(
      graph,
      directory.resolve(s"$name.png"),
      renderingOptions
    )
  }

  def render(name: String, animation: Animation): Unit = {
    val graphs = Graphs.graphs(renderingOptions, animationOptions.onionSkinLayers)(animation)
    AnimatedGifRenderer.renderAnimatedGif(
      graphs,
      SvgGraphAnimation.animate(animationOptions.interpolationFrames),
      directory.resolve(s"$name.gif"),
      renderingOptions,
      animationOptions
    )
  }

  implicit class DiagramRenderSyntax(diagram: Diagram) {
    def render(name: String) = self.render(name, diagram)
  }

  implicit class AnimationRenderSyntax(animation: Animation) {
    def render(name: String) = self.render(name, animation)
  }
}
