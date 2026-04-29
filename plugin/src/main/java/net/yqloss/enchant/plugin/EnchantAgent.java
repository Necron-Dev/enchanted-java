package net.yqloss.enchant.plugin;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
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
          return Enchanter.enchant(classfileBuffer, loader);
        } catch (Throwable throwable) {
          System.err.printf("failed to enchant %s%n", className);
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
