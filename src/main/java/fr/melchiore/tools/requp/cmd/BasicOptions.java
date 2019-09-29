package fr.melchiore.tools.requp.cmd;

import picocli.CommandLine.Option;

abstract class BasicOptions {

  @Option(names = {"-f",
      "--from"}, required = true, description = "directory from where in files will be searched for a pattern")
  String inDirectory;

  @Option(names = {
      "-g", "--glob"}, required = true, description = "glob pattern to be used to select in files")
  String globPattern;

  @Option(names = {"-o",
      "--out"}, required = true, description = "directory where new files will be written")
  String outDirectory;

  @Option(names = "--help", usageHelp = true, description = "display this help and exit")
  boolean help;
}
