package fr.melchiore.tools.requp.cmd;

import com.google.common.collect.Multimap;
import fr.melchiore.tools.requp.data.Requirement;
import fr.melchiore.tools.requp.gen.Generator;
import fr.melchiore.tools.requp.parser.AsciidocParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "simplify", description = "Generate requirements with simplified layout")
public class Simplify extends BasicOptions implements Callable<Integer> {

  @Option(names = {"-s", "--sanitize-tables"}, description = "sanitize table separator in Asciidoc document")
  boolean sanitize;

  @Override
  public Integer call() throws Exception {

    Multimap<Path, Requirement> results = AsciidocParser.from(inDirectory, globPattern);

    if(sanitize) {
      for (Requirement requirement : results.values()) {
        String body = requirement.getBody();

        requirement.setBody(body.replaceAll("(\\n[0-9]*\\.?[0-9]*\\+?[<^>]?\\.?[<^>]?[aehlmdsv]?)!", "$1|"));
      }
    }

    Generator generator = new Generator("simplified_req.ftl", Paths.get(inDirectory),
        Paths.get(outDirectory));

    generator.generate(results, Collections.emptyMap());

    return 0;
  }
}
