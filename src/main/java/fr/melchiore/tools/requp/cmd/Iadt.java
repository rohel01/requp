package fr.melchiore.tools.requp.cmd;

import com.google.common.collect.Multimap;
import fr.melchiore.tools.requp.gen.Generator;
import fr.melchiore.tools.requp.data.Requirement;
import fr.melchiore.tools.requp.parser.AsciidocParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "iadt", description = "Produce iadt matrix for input requirements")
public class Iadt extends BasicOptions implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {

    Multimap<Path, Requirement> results = AsciidocParser.from(inDirectory, globPattern);

    Generator reqGenerator = new Generator("iadt.ftl", Paths.get(inDirectory),
        Paths.get(outDirectory), "iadt.adoc");

    reqGenerator.generate(results, Collections.emptyMap());

    return 0;
  }
}
