package fr.melchiore.tools.requp;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {

        File file = new File(args[0]);

        System.setProperty("jruby.compat.version", "RUBY1_9");
        System.setProperty("jruby.compile.mode", "OFF");
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        Document document = asciidoctor.loadFile(file, Collections.emptyMap());

        // In legacy documents, requirements are top level sections
        List<Section> blocks = document.getBlocks().stream()
                .filter(Section.class::isInstance)
                .map(Section.class::cast)
                .filter(section -> section.hasAttribute("ref") && section.hasAttribute("summary"))
                .collect(Collectors.toList());

        System.out.println(blocks);

        for (Section block : blocks) {

            String ref = (String) block.getAttribute("ref");
            String summary = (String) block.getAttribute("summary");
            String[] subsystem = ((String) block.getAttribute("subsystem", "")).split(",\\s?");
            String verification = (String) block.getAttribute("verification");

            System.out.println("ref: " + ref);
            System.out.println("summary: " + summary);
            System.out.println("subsystem: " + String.join(", ", subsystem));
            System.out.println("verification: " + verification);
            System.out.println("content: " + block.getContent());
            System.out.println("subblocks: " + block.getBlocks());

            block.getBlocks().forEach(b -> {
                System.out.println("\t" + b.getContext());
                System.out.println("\t" + b.getCaption());
                System.out.println("\t" + b.getContent());

                if(b instanceof Table) {
                    Table t = (Table) b;
                    System.out.println("\tTable:" + t.getFrame());
                    System.out.println("\tTable:" + t.getGrid());
                    System.out.println("\tTable:" + t.getReftext());
                    System.out.println("\tTable:" + t.getId());
                }
            });

        }
    }
}
