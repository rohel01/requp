package fr.melchiore.tools.requp;

import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import freemarker.ext.beans.BeansWrapper;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsciidocWriter {

  private static final Logger logger = LoggerFactory.getLogger(AsciidocWriter.class);

  public static void generate(Multimap<Path, Requirement> manifest, Path inPrefix,
      Path outPrefix)
      throws IOException, TemplateModelException {

    Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);

    cfg.setClassForTemplateLoading(AsciidocWriter.class, "/templates");
    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);
    cfg.setWrapUncheckedExceptions(true);
    cfg.setFallbackOnNullLoopVariable(false);

    DefaultObjectWrapper wrapper = new DefaultObjectWrapperBuilder(
        Configuration.VERSION_2_3_29).build();
    TemplateHashModel enumModels = wrapper.getEnumModels();
    TemplateHashModel verificationEnum = (TemplateHashModel) enumModels
        .get(Verification.class.getCanonicalName());

    List<Version> versions = Arrays.asList("0.1", "0.2", "0.3", "1.0").stream()
        .map(Version::valueOf)
        .collect(Collectors.toList());

    Template reqTpl = cfg.getTemplate("requirement.ftl");
    Template iadtTpl = cfg.getTemplate("iadt.ftl");
    Template versionTpl = cfg.getTemplate("version.ftl");

    for (Entry<Path, Collection<Requirement>> entry : manifest.asMap().entrySet()) {
      Path originFile = entry.getKey();
      Collection<Requirement> requirements = entry.getValue();

      Path relativePath = inPrefix.relativize(originFile);

      Path reqPath = Paths.get(outPrefix.toString(), relativePath.toString());
      String outExtension = Files.getFileExtension(reqPath.toString());

      Path iadtPath = Paths.get(reqPath.toString().replace(outExtension, "iadt." + outExtension));
      Path versionPath = Paths
          .get(reqPath.toString().replace(outExtension, "version." + outExtension));

      File reqFile = reqPath.toFile();
      File iadtFile = iadtPath.toFile();
      File versionFile = versionPath.toFile();

      logger.info("Generating " + String
          .join(", ", reqFile.getAbsolutePath(), iadtFile.getAbsolutePath(),
              versionFile.getAbsolutePath()));

      // First, create directories if necessary
      reqFile.getParentFile().mkdirs();

      Map<String, Object> root = new HashMap<>();
      root.put("requirements", requirements);
      root.put("verification", verificationEnum);
      root.put("versions", versions);


      // Then, write requirement
      try (Writer out = new FileWriter(reqFile)) {
        reqTpl.process(root, out);
      } catch (TemplateException e) {
        logger.error("Error when generating " + reqFile.getAbsolutePath(), e);
      }

      // Then, write IADT matrix
      try (Writer out = new FileWriter(iadtFile)) {
        iadtTpl.process(root, out);
      } catch (TemplateException e) {
        logger.error("Error when generating " + reqFile.getAbsolutePath(), e);
      }

      // Then, write Version matrix
      try (Writer out = new FileWriter(versionFile)) {
        versionTpl.process(root, out);
      } catch (TemplateException e) {
        logger.error("Error when generating " + reqFile.getAbsolutePath(), e);
      }
    }
  }


}