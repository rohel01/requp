package fr.melchiore.tools.requp.cmd;

import com.google.common.collect.Multimap;
import fr.melchiore.tools.requp.gen.Generator;
import fr.melchiore.tools.requp.data.Requirement;
import fr.melchiore.tools.requp.parser.LegacyAsciidocParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "migrate", description = "Migrate requirements from legacy to new format")
public class Migrate extends BasicOptions implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {

    Multimap<Path, Requirement> results = LegacyAsciidocParser.from(inDirectory, globPattern);

    Generator generator = new Generator("requirement.ftl", Paths.get(inDirectory),
        Paths.get(outDirectory));

    generator.generate(results, Collections.emptyMap());

    return 0;
  }
}
