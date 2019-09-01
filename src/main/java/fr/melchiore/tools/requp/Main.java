package fr.melchiore.tools.requp;

import com.google.common.collect.Multimap;
import freemarker.template.TemplateModelException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {

  public static void main(String[] args) throws IOException, TemplateModelException {

    String inDirectory = args[0];
    String pattern = args[1];
    String outDirectory = args[2];

    System.out.println(Arrays.asList(args));

    Multimap<Path, Requirement> results = LegacyAsciidocParser.from(inDirectory, pattern);

    AsciidocWriter.generate(results, Paths.get(inDirectory), Paths.get(outDirectory));
  }
}
