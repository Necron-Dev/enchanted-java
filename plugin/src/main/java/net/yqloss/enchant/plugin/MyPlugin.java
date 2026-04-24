package net.yqloss.enchant.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;

public class MyPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().withId(
      "java", plugin -> {
        var javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        javaExtension.getSourceSets().all(sourceSet -> {
          var name = sourceSet.getName();
          var compileTaskName = sourceSet.getCompileJavaTaskName();
          var finalOutputDir = sourceSet.getJava().getClassesDirectory().get().getAsFile();
          var intermediateOutputDir = project.getLayout().getBuildDirectory().dir("intermediates/enchanted-java/" + name).get().getAsFile();
          var enchantTaskName = "enchant" + capitalize(name);

          var compileTaskProvider = project.getTasks().named(
            compileTaskName, JavaCompile.class, compileTask -> {
              compileTask.getDestinationDirectory().set(intermediateOutputDir);
            }
          );

          var enchantTaskProvider = project.getTasks().register(
            enchantTaskName, EnchantTask.class, enchantTask -> {
              enchantTask.getInputDirectory().set(compileTaskProvider.get().getDestinationDirectory());
              enchantTask.getOutputDirectory().set(finalOutputDir);
              enchantTask.getClasspath().from(compileTaskProvider.get().getClasspath());

              enchantTask.onlyIf(task -> {
                var dir = enchantTask.getInputDirectory().getOrNull();
                return dir != null && dir.getAsFile().exists();
              });
            }
          );

          sourceSet.compiledBy(enchantTaskProvider);
        });
      }
    );
  }

  private String capitalize(String string) {
    if (string == null || string.isEmpty()) return string;
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }
}
