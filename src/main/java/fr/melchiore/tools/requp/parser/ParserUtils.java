package fr.melchiore.tools.requp.parser;

import fr.melchiore.tools.requp.data.Compliance;
import fr.melchiore.tools.requp.data.Requirement;
import fr.melchiore.tools.requp.data.Verification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.StructuralNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserUtils {

  static final Logger logger = LoggerFactory.getLogger(ParserUtils.class);

  private ParserUtils() {}

  public static Requirement readAttributes(StructuralNode node) {

    String ref = (String) node.getAttribute("ref");
    String version = (String) node.getAttribute("version");
    String type = (String) node.getAttribute("type");
    String summary = (String) node.getAttribute("summary");
    String[] subsystem = ((String) node.getAttribute("subsystem", "")).split(",\\s?");
    String[] verification = ((String) node.getAttribute("verification"))
        .split(",\\s?");
    String compliance = (String) node.getAttribute("compliance", "TBD");
    String target = (String) node.getAttribute("target", "");


    return new Requirement(
        ref, version, type, summary,
        Collections.emptyList(),
        Arrays.stream(subsystem)
            .collect(Collectors.toList()),
        Arrays.stream(verification)
            .filter(s -> !s.trim().isEmpty())
            .map(Verification::fromAbrv)
            .collect(
                Collectors.toList()),
        Compliance.fromAbrv(compliance),
        target,
        "",
        "");
  }


  public static boolean isInlineComment(Block comment) {
    return !comment.getSource().isEmpty() || comment.getBlocks().isEmpty();
  }

  public static Optional<String> findComments(NodeLocation nl) {
    StructuralNode node = nl.getNode();

    Map<Object, Object> criteria = new HashMap<>();
    criteria.put("context", ":admonition");

    List<Block> commentBlocks = node.findBy(criteria)
        // WARNING: findBy is recursive so nested admonitions might be returned
        .stream()
        .filter(sn -> sn.getParent() == node)
        .map(Block.class::cast)
        .collect(Collectors.toList());

    Optional<String> comments = Optional.empty();
    if (!commentBlocks.isEmpty()) {
      List<String> lines = new ArrayList<>();

      Block firstComment = commentBlocks.get(0);
      nl.end =
          firstComment.getSourceLocation().getLineNumber() - (
              isInlineComment(firstComment) ? 1
                  : 2);

      for (Block admonition : commentBlocks) {
        /*
         * We only support inline admonition blocks for now
         */
        if (!isInlineComment(admonition)) {
          logger.error(
              "Ignoring unsupported " + admonition.getCaption()
                  + " admonition block at "
                  + admonition.getSourceLocation());
          continue;
        }

        lines.add(admonition.getSource());
      }

      comments = Optional.of(String.join("\n\n", lines));
    }

    return comments;
  }

}
