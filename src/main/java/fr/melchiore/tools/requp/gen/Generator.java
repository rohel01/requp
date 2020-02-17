package fr.melchiore.tools.requp.gen;

import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import fr.melchiore.tools.requp.data.Requirement;
import fr.melchiore.tools.requp.data.Verification;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Generator {

  private static final Logger LOGGER;

  private static final Configuration CFG;

  static {
    LOGGER = LoggerFactory.getLogger(Generator.class);
    CFG = new Configuration(Configuration.VERSION_2_3_29);

    CFG.setClassForTemplateLoading(Generator.class, "/templates");
    CFG.setDefaultEncoding("UTF-8");
    CFG.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    CFG.setLogTemplateExceptions(false);
    CFG.setWrapUncheckedExceptions(true);
    CFG.setFallbackOnNullLoopVariable(false);
  }

  private final Template template;
  private final Path inPrefix;
  private final Path outPrefix;
  private final Optional<String> outExtension;

  private Generator(String tplPath, Path inPrefix,
      Path outPrefix, Optional<String> outExtension) throws IOException {
    this.template = CFG.getTemplate(tplPath);
    this.inPrefix = inPrefix;
    this.outPrefix = outPrefix;
    this.outExtension = outExtension;
  }

  public Generator(String tplPath, Path inPrefix,
      Path outPrefix, String outExtension) throws IOException {
    this(tplPath, inPrefix, outPrefix, Optional.of(outExtension));
  }

  public Generator(String tplPath, Path inPrefix) throws IOException {
    this(tplPath, inPrefix, inPrefix, Optional.empty());
  }

  public Generator(String tplPath, Path inPrefix, Path outPrefix) throws IOException {
    this(tplPath, inPrefix, outPrefix, Optional.empty());
  }

  public void generate(Multimap<Path, Requirement> manifest, Map<String, Object> intitalData)
      throws TemplateModelException, IOException {

    DefaultObjectWrapper wrapper = new DefaultObjectWrapperBuilder(
        Configuration.VERSION_2_3_29).build();
    TemplateHashModel enumModels = wrapper.getEnumModels();
    TemplateHashModel verificationEnum = (TemplateHashModel) enumModels
        .get(Verification.class.getCanonicalName());

    for (Entry<Path, Collection<Requirement>> entry : manifest.asMap().entrySet()) {
      Path inPath = entry.getKey();
      Collection<Requirement> requirements = entry.getValue();

      String relativePath = "";
      if(inPrefix.toFile().isFile()) {
        relativePath = inPath.getFileName().toString();
      }
      else {
        // Compute relative path
        relativePath = inPrefix
            .relativize(inPath).toString();
      }

      if (this.outExtension.isPresent()) {
        String inExtension = Files.getFileExtension(inPath.toString());
        relativePath = relativePath.replace(inExtension, outExtension.get());
      }

      // Compute output file path
      Path outPath = Paths.get(outPrefix.toString(), relativePath);
      File outFile = outPath.toFile();

      LOGGER.info("Generating " + outFile + " from " + inPath);

      // First, create directories if necessary
      outFile.getParentFile().mkdirs();

      Map<String, Object> root = new HashMap<>(intitalData);
      root.put("requirements", requirements);
      root.put("verification", verificationEnum);

      // Then, write output file
      try (Writer out = new FileWriter(outFile)) {
        this.template.process(root, out);
      } catch (TemplateException e) {
        LOGGER.error("Error when generating " + outFile.getAbsolutePath(), e);
      }
    }
  }

}
