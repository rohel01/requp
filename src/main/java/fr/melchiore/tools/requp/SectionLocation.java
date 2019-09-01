package fr.melchiore.tools.requp;

import org.asciidoctor.ast.Section;

class SectionLocation {

  Section getSection() {
    return section;
  }

  final Section section;
  int start;
  int end;

  SectionLocation(Section section, int start, int end) {
    this.section = section;
    this.start = start;
    this.end = end;
  }

  @Override
  public String toString() {
    return String.format("{%d..%d}", start, end);
  }
}
