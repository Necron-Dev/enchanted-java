package net.yqloss.enchant.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.tasks.*;
import org.gradle.work.ChangeType;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@CacheableTask
public abstract class EnchantTask extends DefaultTask {
  @Incremental
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputDirectory
  @Optional
  public abstract DirectoryProperty getInputDirectory();

  @OutputDirectory
  public abstract DirectoryProperty getOutputDirectory();

  @TaskAction
  public void execute(InputChanges inputChanges) throws IOException {
    var inputDir = getInputDirectory();
    if (!inputDir.isPresent() || !inputDir.get().getAsFile().exists()) return;
    var outputDir = getOutputDirectory().get().getAsFile();
    for (var change : inputChanges.getFileChanges(inputDir)) {
      if (change.getFileType() == FileType.DIRECTORY) continue;
      var sourceFile = change.getFile();
      var targetFile = new File(outputDir, change.getNormalizedPath());
      if (change.getChangeType() == ChangeType.REMOVED) {
        var ignored = targetFile.delete();
        continue;
      }
      var ignored = targetFile.getParentFile().mkdirs();
      if (!sourceFile.getName().endsWith(".class")) {
        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        continue;
      }
      var content = Files.readAllBytes(sourceFile.toPath());
      Files.write(targetFile.toPath(), Enchanter.enchant(content));
    }
  }
}
