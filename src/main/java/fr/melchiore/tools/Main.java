package fr.melchiore.tools;

import com.vladsch.flexmark.convert.html.FlexmarkHtmlParser;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.jira.converter.JiraConverterExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {

        File file = new File(args[1]);

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        Document document = asciidoctor.loadFile(file, Collections.emptyMap());

        // Initialize attributes with header content
        Map<String, Object> attributes = new HashMap<>(document.getAttributes());

        System.out.println(document.getAttributes());
        System.out.println(document.getOptions());
        System.out.println(document.getSubstitutions());


        for (StructuralNode node : document.getBlocks()) {

            // Then overwrite with section attributes
            Map<String, Object> nodeAttributes = node.getAttributes();
            attributes.putAll(nodeAttributes);

            System.out.println("NODE");
            System.out.println(node.getAttributes());
/*            System.out.println(node.getBlocks());

            for (StructuralNode block : node.getBlocks()) {
                System.out.println("SUBNODE");
                System.out.println(block.getAttributes());
                System.out.println(block.getBlocks());
            }*/

            String id = (String) node.getAttribute("id");
            String reqVersion = (String) node.getAttribute("reqVersion");
            String title = (String) node.getAttribute("title");
            String[] subsystem = ((String) node.getAttribute("subsystem")).split(",\\s?");
            String[] satisfies = ((String) node.getAttribute("satisfies")).split(",\\s?");
            String verification = (String) node.getAttribute("verification");
            String compliance = (String) node.getAttribute("compliance");
            String allocVersion = (String) node.getAttribute("allocVersion");

            String nodeContent = (String) node.getContent();

            String commonMarkup = FlexmarkHtmlParser.parse(nodeContent);

            MutableDataSet options = new MutableDataSet().set(Parser.EXTENSIONS,
                    Arrays.asList(
                            JiraConverterExtension.create(),
                            TablesExtension.create()));

            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();

            Node astDocument = parser.parse(commonMarkup);
            String jiraMarkup = renderer.render(astDocument);

            System.out.println(title);
            System.out.println(jiraMarkup);
            System.out.println();
        }
    }
}
