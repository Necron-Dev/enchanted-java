package net.yqloss.enchant.plugin;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.Arrays;

public final class EnchantAgent {
  private EnchantAgent() {
  }

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    install(instrumentation);
  }

  public static void agentmain(String agentArgs, Instrumentation instrumentation) {
    install(instrumentation);
  }

  private static void install(Instrumentation instrumentation) {
    var prefixes = readPrefixes();
    var classesDirectory = new File(System.getProperty("yqloss.enchant.classes"));

    ClassFileTransformer transformer = new ClassFileTransformer() {
      @Override
      public byte[] transform(
        Module module,
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
      ) {
        if (className == null || !matches(prefixes, className)) return null;

        try {
          var file = new File(classesDirectory, className.replace('.', '/') + ".class");
          if (!file.exists() || !file.isFile()) {
            System.err.printf("class file not found: %s%n", className);
            return null;
          }
          System.err.printf("loaded class: %s%n", className);
          return Files.readAllBytes(file.toPath());
        } catch (Throwable throwable) {
          System.err.printf("failed to load class: %s%n", className);
          throwable.printStackTrace();
          return null;
        }
      }
    };

    instrumentation.addTransformer(transformer, instrumentation.isRetransformClassesSupported());
  }

  private static String[] readPrefixes() {
    var value = System.getProperty("yqloss.enchant.prefixes");
    if (value == null) return null;

    return Arrays
             .stream(value.split(","))
             .map(String::trim)
             .filter(prefix -> !prefix.isEmpty())
             .map(prefix -> prefix.replace('.', '/'))
             .toArray(String[]::new);
  }

  private static boolean matches(String[] prefixes, String className) {
    if (prefixes == null) return true;

    for (var prefix : prefixes) {
      if (className.startsWith(prefix)) return true;
    }

    return false;
  }
}
