package net.yqloss.enchant.plugin;

import net.yqloss.enchant.plugin.pass.CheckerPass;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.enchant.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Set;

public class Enchanter {
  public static final Set<String> EnchantedJavaClasses = Set.of(
    "net/yqloss/enchant/library/EnchantedJava",
    "net/yqloss/enchant/library/Ench",
    "net/yqloss/enchant/library/E"
  );

  public static final List<Pass> EnchantPasses = List.of(
    SafePass.Instance,
    ElvisPass.Instance,
    PassPass.Instance,
    ConstantsPass.Instance,
    ScopeFunctionPass.Instance,
    NeverPass.Instance,
    ThrowPass.Instance,
    ReturnPass.Instance,
    CheckerPass.Instance
  );

  public static final List<Pass> OptimizePasses = List.of(
  );

  public static byte[] enchant(byte[] original, ClassLoader classLoader) {
    var cr = new ClassReader(original);
    var cn = new ClassNode();
    cr.accept(cn, 0);
    EnchantPasses.forEach(pass -> pass.accept(cn));
    while (OptimizePasses.stream().anyMatch(pass -> pass.accept(cn))) ;
    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
      @Override
      protected String getCommonSuperClass(String type1, String type2) {
        try {
          var c = Class.forName(type1.replace('/', '.'), false, classLoader);
          var d = Class.forName(type2.replace('/', '.'), false, classLoader);
          if (c.isAssignableFrom(d)) return type1;
          if (d.isAssignableFrom(c)) return type2;
          if (c.isInterface() || d.isInterface()) return "java/lang/Object";
          do {
            c = c.getSuperclass();
          } while (!c.isAssignableFrom(d));
          return c.getName().replace('.', '/');
        } catch (Exception e) {
          System.err.printf("couldn't find common super class for %s and %s\n", type1, type2);
          return "java/lang/Object";
        }
      }
    };
    cn.accept(cw);
    return cw.toByteArray();
  }
}
