package net.yqloss.enchant.plugin;

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
    PassPass.Instance,
    ConstantsPass.Instance,
    NeverPass.Instance,
    ThrowPass.Instance,
    ReturnPass.Instance
  );

  public static final List<Pass> OptimizePasses = List.of(
  );

  public static byte[] enchant(byte[] original) {
    var cr = new ClassReader(original);
    var cn = new ClassNode();
    cr.accept(cn, 0);
    EnchantPasses.forEach(pass -> pass.accept(cn));
    while (OptimizePasses.stream().anyMatch(pass -> pass.accept(cn))) ;
    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }
}
