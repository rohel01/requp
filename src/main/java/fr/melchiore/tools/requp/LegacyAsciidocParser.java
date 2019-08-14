package fr.melchiore.tools.requp;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class LegacyAsciidocParser {

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

        return LegacyAsciidocParser.from(file);
    }

    public static List<Requirement> from(File file) {
        List<Requirement> result = new ArrayList<>();

        if(file.isFile()) {
            Document document = asciidoctor.loadFile(file, Collections.emptyMap());

            // In legacy documents, requirements are top level sections
            List<Section> blocks = document.getBlocks().stream()
                    .filter(Section.class::isInstance)
                    .map(Section.class::cast)
                    .filter(section -> section.hasAttribute("ref") && section.hasAttribute("summary"))
                    .collect(Collectors.toList());

            blocks.stream()
                    .map(LegacyAsciidocParser::from)
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
            return LegacyAsciidocParser.from(file);

        List<Requirement> result = new ArrayList<>();

        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

        SimpleFileVisitor<Path> treeVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {

                // Only work with regular files that matches the provided pattern
                if(basicFileAttributes.isRegularFile() && matcher.matches(path)) {
                    File file = path.toFile();

                    result.addAll(LegacyAsciidocParser.from(file));
                }

                return FileVisitResult.CONTINUE;
            }
        };

        Path startingPath = Paths.get(path);
        Files.walkFileTree(startingPath, treeVisitor);

        return result;
    }

    private static Optional<Requirement> from(Section section) {
        Optional<Requirement> result;

        String ref = (String) section.getAttribute("ref");
        String summary = (String) section.getAttribute("summary");
        String[] subsystem = ((String) section.getAttribute("subsystem", "")).split(",\\s?");
        String verification = (String) section.getAttribute("verification");



        String body = "";
        String note = "";

        result = Optional.of(
                new Requirement(
                        ref, "1", "TBD", summary,
                        Collections.emptyList(),
                        Arrays.stream(subsystem)
                                .collect(Collectors.toList()),
                        Requirement.Verification.fromAbrv(verification),
                        Requirement.Compliance.UNSET,
                        "",
                        body,
                        note));

        return result;
    }
}
