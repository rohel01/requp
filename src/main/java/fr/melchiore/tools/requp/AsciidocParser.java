package fr.melchiore.tools.requp;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AsciidocParser {

    static final Asciidoctor asciidoctor;

    static {
        // Useful to speed-up JRuby startup
        System.setProperty("jruby.compat.version", "RUBY1_9");
        System.setProperty("jruby.compile.mode", "OFF");
        asciidoctor = Asciidoctor.Factory.create();
    }

    public static List<Requirement> from(String path) {
        List<Requirement> result = new ArrayList<>();

        File file = new File(path);

        return AsciidocParser.from(file);
    }

    public static List<Requirement> from(File file) {
        List<Requirement> result = new ArrayList<>();

        if(file.isFile()) {
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
        }

        return result;
    }


    public static List<Requirement> from(String path, String globPattern) throws IOException {

        File file = new File(path);

        if(file.isFile())
            // It is a file, fall back to file method
            return AsciidocParser.from(file);

        List<Requirement> result = new ArrayList<>();

        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

        SimpleFileVisitor<Path> treeVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {

                // Only work with regular files that matches the provided pattern
                if(basicFileAttributes.isRegularFile() && matcher.matches(path)) {
                    File file = path.toFile();

                    result.addAll(AsciidocParser.from(file));
                }

                return FileVisitResult.CONTINUE;
            }
        };

        Path startingPath = Paths.get(path);
        Files.walkFileTree(startingPath, treeVisitor);

        return result;
    }

    private static Optional<Requirement> from(Table table) {
        Optional<Requirement> result;

        String ref = (String) table.getAttribute("ref");
        String version = (String) table.getAttribute("version");
        String type = (String) table.getAttribute("type");
        String summary = (String) table.getAttribute("summary");
        String[] subsystem = ((String) table.getAttribute("subsystem", "")).split(",\\s?");
        String verification = (String) table.getAttribute("verification");
        String compliance = (String) table.getAttribute("compliance");
        String target = (String) table.getAttribute("target");

        // Row 1 is title + version
        // Row 2 is Satisfies + Verification
        // Row 3 is Satisfies + Compliance
        // Row 4 is Requirement text
        // Row 5 is Requirement notes

        List<Row> rows = table.getBody();
        assert rows.size() == 5;

        Row secondRow = rows.get(1);
        List<Cell> secondCells = secondRow.getCells();
        assert secondCells.size() == 4;
        Cell satisfyCell = secondCells.get(1);

        Row fourthRow = rows.get(3);
        List<Cell> fourthCells = fourthRow.getCells();
        assert fourthCells.size() == 1;
        Cell textCell = fourthCells.get(0);


        Row fifthRow = rows.get(4);
        List<Cell> fifthCells = fifthRow.getCells();
        assert fifthCells.size() == 2;
        Cell noteCell = fifthCells.get(1);


        List<StructuralNode> satisfyBlocks = satisfyCell.getInnerDocument().getBlocks();
        assert  satisfyBlocks.size() == 1;

        List<String> satisfies = new ArrayList<>();
        org.asciidoctor.ast.List satisfyList = (org.asciidoctor.ast.List) satisfyBlocks.get(0);
        satisfyList.getItems().forEach(sn -> satisfies.add(((ListItem) sn).getText()));

        String note = noteCell.getText();

        String body = textCell.getText();

        result = Optional.of(
                new Requirement(
                        ref, version, type, summary,
                        satisfies,
                        Arrays.stream(subsystem)
                                .collect(Collectors.toList()),
                        Requirement.Verification.fromAbrv(verification),
                        Requirement.Compliance.fromAbrv(compliance),
                        target,
                        body,
                        note));

        return result;
    }
}
