package fr.melchiore.tools.requp.parser;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsciidocParser {

  static final Logger logger = LoggerFactory.getLogger(AsciidocParser.class);

  static final Asciidoctor asciidoctor;

  private static final Map<Integer, FormatMode> sizeToFormat;

  private static final int N_COLUMNS_FULL = 8;
  private static final int N_COLUMNS_SHORT = 6;

  static {
    // Useful to speed-up JRuby startup
    System.setProperty("jruby.compat.version", "RUBY1_9");
    System.setProperty("jruby.compile.mode", "OFF");
    asciidoctor = Asciidoctor.Factory.create();

    sizeToFormat = new HashMap<>();
    sizeToFormat.put(N_COLUMNS_FULL, FormatMode.FULL);
    sizeToFormat.put(N_COLUMNS_SHORT, FormatMode.SHORT);
  }

  private AsciidocParser() {
  }

  public static List<Requirement> from(String path) throws IOException {
    List<Requirement> result = new ArrayList<>();

    File file = new File(path);

    return AsciidocParser.from(file);
  }

  public static List<Requirement> from(File file) throws IOException {
    List<Requirement> result = new ArrayList<>();

    if (file.isFile()) {
      Document document = asciidoctor.loadFile(file, Collections.emptyMap());

      Map<Object, Object> map = new HashMap<>();
      map.put("context", ":table");
      map.put("role", ":requirement");

      List<StructuralNode> blocks = document.findBy(map);

      blocks.stream()
          .map(Table.class::cast)
          .map(AsciidocParser::from)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .forEach(result::add);
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
      result.putAll(file.toPath(), AsciidocParser.from(file));
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

          result.putAll(path, AsciidocParser.from(file));
        }

        return FileVisitResult.CONTINUE;
      }
    };

    Path startingPath = Paths.get(path);
    logger.debug("Starting path is: " + startingPath);
    Files.walkFileTree(startingPath, treeVisitor);

    return result;
  }

  private static Optional<Requirement> from(Table table) {
    Optional<Requirement> result;

    Requirement attributes = ParserUtils.readAttributes(table);

    // There are two formats for requirements FULL and SHORT
    // The short format optimizes space no traceability links are provided
    FormatMode formatMode = sizeToFormat.get(table.getColumns().size());

    Cell textCell;
    Cell noteCell;
    List<String> satisfies = new ArrayList<>();

    List<Row> rows = table.getBody();
    switch (formatMode) {
      case FULL: {
        // Row 1 is title + version
        // Row 2 is Satisfies + Verification
        // Row 3 is Satisfies + Compliance
        // Row 4 is Requirement text
        // Row 5 is Requirement notes
        assert rows.size() == 5;

        Row secondRow = rows.get(1);
        List<Cell> secondCells = secondRow.getCells();
        assert secondCells.size() == 4;
        Cell satisfyCell = secondCells.get(1);

        Row fourthRow = rows.get(3);
        List<Cell> fourthCells = fourthRow.getCells();
        assert fourthCells.size() == 1;
        textCell = fourthCells.get(0);

        Row fifthRow = rows.get(4);
        List<Cell> fifthCells = fifthRow.getCells();
        assert fifthCells.size() == 2;
        noteCell = fifthCells.get(1);

        List<StructuralNode> satisfyBlocks = satisfyCell.getInnerDocument().getBlocks();

        if (!satisfyBlocks.isEmpty()) {
          assert satisfyBlocks.size() == 1;
          org.asciidoctor.ast.List satisfyList = (org.asciidoctor.ast.List) satisfyBlocks.get(0);
          satisfyList.getItems().forEach(sn -> satisfies.add(((ListItem) sn).getText()));
        }
        break;
      }

      case SHORT: {
        // Row 1 is title
        // Row 2 is Version + Verification + Compliance
        // Row 3 is Requirement text
        // Row 4 is Requirement notes
        assert rows.size() == 4;

        Row textRow = rows.get(2);
        List<Cell> textCells = textRow.getCells();
        assert textCells.size() == 1;
        textCell = textCells.get(0);

        Row noteRow = rows.get(3);
        List<Cell> noteCells = noteRow.getCells();
        assert noteCells.size() == 2;
        noteCell = noteCells.get(1);
        break;
      }

      default:
        throw new IllegalStateException("Unexpected value: " + formatMode);
    }

    String note = noteCell.getText();
    String body = textCell.getText();

    result = Optional.of(
        Requirement.fromAttributes(attributes, satisfies, body, note));

    return result;
  }

  private enum FormatMode {
    FULL,
    SHORT
  }
}
