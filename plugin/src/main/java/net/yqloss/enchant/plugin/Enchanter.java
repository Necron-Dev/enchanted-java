package net.yqloss.enchant.plugin;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.CheckerPass;
import net.yqloss.enchant.plugin.pass.enchant.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Enchanter {
  public static final Set<String> EnchantedJavaClasses = Set.of(
    "yqloss/E"
  );

  public static ClassNode bytesToClassNode(byte[] data) {
    var cr = new ClassReader(data);
    var cn = new ClassNode();
    cr.accept(cn, 0);
    return cn;
  }

  public static byte[] classNodeToBytes(ClassNode cn, ClassLoader classLoader) {
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
          e.printStackTrace();
          return "java/lang/Object";
        }
      }
    };
    cn.accept(cw);
    return cw.toByteArray();
  }

  public static byte[] enchant(byte[] original, ClassLoader classLoader, Map<String, ?> properties) {
    var cn = bytesToClassNode(original);
    try {
      List.of(
        ArgPass.Instance,
        DefaultArgsPass.Instance,
        ElvisPass.Instance,
        SafePass.Instance,
        PassCastPass.Instance,
        ConstantsPass.Instance,
        ScopeFunctionPass.Instance,
        NeverPass.Instance,
        ThrowPass.Instance,
        ReturnPass.Instance,
        new PropertyPass(properties),
        CompileTimePass.Instance,
        TrimAnnotationPass.Instance,
        CheckerPass.Instance
      ).forEach(pass -> pass.accept(cn, classLoader));
      return classNodeToBytes(cn, classLoader);
    } catch (Exception exception) {
      exception.printStackTrace();
      AsmHelper.debugClass(cn);
      throw exception;
    }
  }
}
