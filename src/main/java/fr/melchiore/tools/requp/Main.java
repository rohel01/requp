package fr.melchiore.tools.requp;

import fr.melchiore.tools.requp.cmd.Iadt;
import fr.melchiore.tools.requp.cmd.Migrate;
import fr.melchiore.tools.requp.cmd.Version;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(subcommands = {
    Migrate.class,
    Version.class,
    Iadt.class}
)
public class Main implements Callable<Integer> {

  @Option(names = "--help", usageHelp = true, description = "display this help and exit")
  boolean help;

  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new Main());

    commandLine.registerConverter(com.github.zafarkhaja.semver.Version.class,
        com.github.zafarkhaja.semver.Version::valueOf);

    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    return 0;
  }
}
