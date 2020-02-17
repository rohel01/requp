package fr.melchiore.tools.requp.cmd;

import com.google.common.collect.Multimap;
import fr.melchiore.tools.requp.data.Requirement;
import fr.melchiore.tools.requp.gen.Generator;
import fr.melchiore.tools.requp.parser.AsciidocParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "version", description = "Produce version compliance matrix for input requirements")
public class Version extends BasicOptions implements Callable<Integer> {

  @Parameters(description = "VERSIONs to be used in the version matrix", arity = "1..*", paramLabel = "VERSION")
  private List<com.github.zafarkhaja.semver.Version> versions;


  @Override
  public Integer call() throws Exception {

    Multimap<Path, Requirement> results = AsciidocParser.from(inDirectory, globPattern);

    Generator generator = new Generator("version.ftl", Paths.get(inDirectory),
        Paths.get(outDirectory), "version.adoc");

    // Add the target versions to the template data models
    Map<String, Object> initialData = new HashMap<>();
    initialData.put("versions", versions);

    generator.generate(results, initialData);

    return 0;
  }
}
