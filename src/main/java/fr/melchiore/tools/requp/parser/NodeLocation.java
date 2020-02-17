package fr.melchiore.tools.requp.parser;

import org.asciidoctor.ast.StructuralNode;

class NodeLocation {

  StructuralNode getNode() {
    return node;
  }

  final StructuralNode node;
  int start;
  int end;

  NodeLocation(StructuralNode node, int start, int end) {
    this.node = node;
    this.start = start;
    this.end = end;
  }

  @Override
  public String toString() {
    return String.format("{%d..%d}", start, end);
  }
}
