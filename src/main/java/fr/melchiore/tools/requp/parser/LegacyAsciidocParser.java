package fr.melchiore.tools.requp.parser;

import static fr.melchiore.tools.requp.parser.ParserUtils.isInlineComment;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import fr.melchiore.tools.requp.data.Requirement;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cursor;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyAsciidocParser {

  static final Asciidoctor asciidoctor;

  static final Logger logger = LoggerFactory.getLogger(LegacyAsciidocParser.class);

  private LegacyAsciidocParser() {
  }

  static {
    // Useful to speed-up JRuby startup
    System.setProperty("jruby.compat.version", "RUBY1_9");
    System.setProperty("jruby.compile.mode", "OFF");
    asciidoctor = Asciidoctor.Factory.create();
  }

  public static List<Requirement> from(String path) throws IOException {
    List<Requirement> result = new ArrayList<>();

    File file = new File(path);

    return LegacyAsciidocParser.from(file);
  }

  public static List<Requirement> from(File file) throws IOException {
    List<Requirement> result = new ArrayList<>();

    if (file.isFile()) {
      Document document = asciidoctor
          .loadFile(file, OptionsBuilder.options().sourcemap(true).asMap());

      // In legacy documents, requirements are top level sections
      List<Section> blocks = document.getBlocks().stream()
          .filter(Section.class::isInstance)
          .map(Section.class::cast)
          .filter(section -> section.hasAttribute("ref") && section.hasAttribute("summary"))
          .collect(Collectors.toList());

      Set<String> files = new HashSet<>();

      // We use mutable entry since we might update them when looking for comments
      List<NodeLocation> sectionLocations = Streams
          .zip(blocks.stream(), blocks.stream().skip(1), AbstractMap.SimpleEntry::new)
          .map(e -> {
            Cursor slStart = e.getKey().getSourceLocation();
            Cursor slEnd = e.getValue().getSourceLocation();

            files.add(slStart.getFile());

            int start = slStart.getLineNumber() + 1; // + 1 to skip the section header
            int end = slEnd.getLineNumber() - 1; // -1 to skip section attribute

            return new NodeLocation(e.getKey(), start, end);
          }).collect(Collectors.toList());

      if (!sectionLocations.isEmpty()) {
        // Ensure all locations belongs to the same file
        if (files.size() != 1) {
          logger.error(
              "Requirements should be located into a single file (" + files + "). Stopping...");
          return Collections.emptyList();
        }

        List<String> fileLines = com.google.common.io.Files.asCharSource(file, Charsets.UTF_8)
            .readLines();

        // Since all blocks belong to a single file, it makes sense to compare line numbers
        // Add end entry for last section
        Section lastSection = Iterables.getLast(blocks);
        sectionLocations.add(
            new NodeLocation(lastSection,
                lastSection.getSourceLocation().getLineNumber() + 1,
                fileLines.size()));

        /*
         * Here, we compute the requirement body and optional comments
         * Comments are admonitions in the requirement section
         * Also, we need to remove attribute definitions from the next requirement because of how attributes are parsed in Asciidoc
         */
        Map<Object, Object> map = new HashMap<>();
        map.put("context", ":admonition");

        for (NodeLocation sl : sectionLocations) {

          Section requirement = (Section) sl.getNode();

          Requirement attributes = ParserUtils.readAttributes(requirement);
          Optional<String> comments = ParserUtils.findComments(sl);

          // We need to remove attribute definitions
          sl.end = IntStream.range(sl.start, sl.end)
              .filter(i -> fileLines.get(i).matches("^:\\S+:( .*)?"))
              .findFirst().orElse(sl.end);

          List<String> lines = fileLines.subList(sl.start, sl.end);

          // We must change separator of nested tables
          Map<Object, Object> tableSelector = new HashMap<>();
          tableSelector.put("context", ":table");
          List<StructuralNode> nestedTables = requirement.findBy(tableSelector);

          // We support a single level of table nesting so warn user
          AtomicBoolean hasDeepTables = new AtomicBoolean(false);
          nestedTables.stream()
              .filter(s -> s.getParent() != requirement)
              .findAny()
              .ifPresent(s -> {
                logger.warn("Deep nesting of tables detected ! Ignoring them !");
                hasDeepTables.set(true);
              });

          if (!nestedTables.isEmpty() && !hasDeepTables.get()) {
            lines = lines.stream().map(s -> s.replaceFirst("\\|", "!"))
                .collect(Collectors.toList());

            // Add an empty line to circumvent a nested table formatting atefact
            // when a table is the last element of a cell
            List<StructuralNode> subBlocks = requirement.getBlocks();
            if (!subBlocks.isEmpty() && Iterables.getLast(subBlocks) instanceof Table) {
              lines.add("{empty}");
            }
          }

          String body = String.join("\n", lines);

          result.add(
              Requirement.fromAttributes(attributes,
                  Collections.emptyList(), body, comments.orElse("")));
        }
      } else {
        logger.warn("No requirements found in " + file);
      }
    } else {
      logger.warn(file + " is not a directory. Walking its tree...");
      AsciidocParser.from(file.getPath(), "**");
    }

    return result;
  }


  public static Multimap<Path, Requirement> from(String path, String globPattern)
      throws IOException {

    Multimap<Path, Requirement> result = ArrayListMultimap.create();

    File file = new File(path);

    // It is a file, fall back to file method
    if (file.isFile()) {
      logger.warn("Directory expected, found file instead");
      result.putAll(file.toPath(), LegacyAsciidocParser.from(file));
      return result;
    }

    String glob = "glob:" + path + globPattern;
    PathMatcher matcher =
        FileSystems.getDefault().getPathMatcher(glob);

    logger.debug("Glob pattern is: " + glob);

    SimpleFileVisitor<Path> treeVisitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
          throws IOException {
        logger.debug("Considering path: " + path);

        // Only work with regular files that matches the provided pattern
        if (basicFileAttributes.isRegularFile() && matcher.matches(path)) {
          File file = path.toFile();

          result.putAll(path, LegacyAsciidocParser.from(file));
        }

        return FileVisitResult.CONTINUE;
      }
    };

    Path startingPath = Paths.get(path);
    logger.debug("Starting path is: " + startingPath);
    Files.walkFileTree(startingPath, treeVisitor);

    return result;
  }
}
