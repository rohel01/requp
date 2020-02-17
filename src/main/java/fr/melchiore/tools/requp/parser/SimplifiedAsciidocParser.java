package fr.melchiore.tools.requp.parser;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
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
import org.asciidoctor.ast.DescriptionList;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplifiedAsciidocParser {

  static final Logger logger = LoggerFactory.getLogger(SimplifiedAsciidocParser.class);

  static final Asciidoctor asciidoctor;

  static {
    // Useful to speed-up JRuby startup
    System.setProperty("jruby.compat.version", "RUBY1_9");
    System.setProperty("jruby.compile.mode", "OFF");
    asciidoctor = Asciidoctor.Factory.create();
  }

  private SimplifiedAsciidocParser() {
  }

  public static List<Requirement> from(String path) throws IOException {
    List<Requirement> result = new ArrayList<>();

    File file = new File(path);

    return SimplifiedAsciidocParser.from(file);
  }

  public static List<Requirement> from(File file) throws IOException {
    List<Requirement> result = new ArrayList<>();

    if (file.isFile()) {
      Document document = asciidoctor.loadFile(file, OptionsBuilder.options().sourcemap(true).asMap());

      Map<Object, Object> map = new HashMap<>();
      map.put("role", ":requirement");

      // In simplified format, requirements are enclosed in anonymous blocks
      List<StructuralNode> requirements = document.findBy(map);

      List<String> fileLines = com.google.common.io.Files.asCharSource(file, Charsets.UTF_8)
          .readLines();

      List<Block> validatedRequirements = requirements.stream()
          .map(Block.class::cast)
          .filter(block -> {
            List<StructuralNode> reqBlocks = block.getBlocks();

            boolean hasContent = reqBlocks.size() >= 2;
            boolean hasHeader = hasContent && reqBlocks.get(0) instanceof DescriptionList;

            return hasContent && hasHeader;
          }).collect(Collectors.toList());

      requirements.removeAll(validatedRequirements);

      if (!requirements.isEmpty()) {
        StringBuilder warning = new StringBuilder("Invalid requirement nodes have been removed;\n");
        warning.append(requirements.stream()
            .map(StructuralNode::getSourceLocation)
            .map(Cursor::toString)
            .collect(
                Collectors.joining("\n\t")));
        logger.warn(warning.toString());
      }

      Set<String> files = new HashSet<>();

      // We use mutable entry since we might update them when looking for comments
      List<NodeLocation> blockLocations = validatedRequirements.stream()
          .map(block -> {
            List<StructuralNode> reqBlocks = block.getBlocks();

            // Use first real content to remove header
            StructuralNode firstContent = reqBlocks.get(1);

            Cursor slStart = firstContent.getSourceLocation();
            int start = slStart.getLineNumber();

            int end = fileLines.size();
            StructuralNode parent = (StructuralNode) block.getParent();
            StructuralNode lastSibling = Iterables.getLast(parent.getBlocks());
            if (block != lastSibling) {
              List<StructuralNode> siblings = parent.getBlocks();
              StructuralNode nextSibling = siblings.get(siblings.indexOf(block) + 1);
              end = nextSibling.getSourceLocation().getLineNumber();
            }
            else {
              // Try to use the next parent sibling
              StructuralNode grandParent = (StructuralNode) parent.getParent();
              lastSibling = Iterables.getLast(grandParent.getBlocks());
              if(parent != lastSibling)
              {
                List<StructuralNode> grandSiblings = grandParent.getBlocks();
                StructuralNode nextSibling = grandSiblings.get(grandSiblings.indexOf(parent) + 1);
                end = nextSibling.getSourceLocation().getLineNumber();
              }
            }

            // We need to remove attribute definitions and block ends
            end = IntStream.range(start, end)
                .filter(i -> fileLines.get(i).matches("^:\\S+:( .*)?") || fileLines.get(i).matches("^--$"))
                .findFirst().orElse(end);

            files.add(slStart.getFile());

            return new NodeLocation((Block) block, start, end);
          }).collect(Collectors.toList());

      // Ensure all locations belongs to the same file
      if (!blockLocations.isEmpty() && files.size() != 1) {
        logger.error(
            "Requirements should be located into a single file (" + files + "). Stopping...");
        return Collections.emptyList();
      }

      /*
       * Here, we compute the requirement body and optional comments
       * Comments are admonitions in the requirement blocks
       */
      for (NodeLocation sl : blockLocations) {
        Block requirement = (Block) sl.getNode();

        Requirement attributes = ParserUtils.readAttributes(requirement);
        Optional<String> comments = ParserUtils.findComments(sl);

        List<String> lines = fileLines.subList(sl.start - 1, sl.end);

        String body = String.join("\n", lines);

        result.add(Requirement
            .fromAttributes(attributes, Collections.emptyList(), body, comments.orElse("")));
      }
    } else {
      logger.warn(file + " is not a directory. Walking its tree...");
      SimplifiedAsciidocParser.from(file.getPath(), "**");
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
      result.putAll(file.toPath(), SimplifiedAsciidocParser.from(file));
      return result;
    }

    PathMatcher matcher =
        FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

    SimpleFileVisitor<Path> treeVisitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
          throws IOException {
        logger.debug("Considering path: " + path);

        // Only work with regular files that matches the provided pattern
        if (basicFileAttributes.isRegularFile() && matcher.matches(path)) {
          File file = path.toFile();

          result.putAll(path, SimplifiedAsciidocParser.from(file));
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
