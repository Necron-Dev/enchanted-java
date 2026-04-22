package net.yqloss.enchant.plugin.pass;

import net.yqloss.enchant.plugin.Enchanter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public enum CheckerPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn) {
    for (var mn : cn.methods) {
      var th = new ThrowHelper("checker", cn, mn);
      for (var insn : mn.instructions) {
        if (insn instanceof FieldInsnNode min
            && min.getOpcode() == Opcodes.GETSTATIC
            && Enchanter.EnchantedJavaClasses.contains(min.owner)
        ) {
          throw th.raise("enchanted field %s is not replaced", min.name);
        }
        if (insn instanceof MethodInsnNode min
            && min.getOpcode() == Opcodes.INVOKESTATIC
            && Enchanter.EnchantedJavaClasses.contains(min.owner)
        ) {
          throw th.raise("enchanted method %s%s is not replaced", min.name, min.desc);
        }
      }
    }
    return false;
  }
}
