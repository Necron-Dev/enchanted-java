package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public enum PassCastPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && min.getOpcode() == Opcodes.INVOKESTATIC
          && Enchanter.EnchantedJavaClasses.contains(min.owner)
          && ("_pass".equals(min.name) || "_cast".equals(min.name))
        ) {
          modified = true;
          iter.remove();
        }
      }
    }
    return modified;
  }
}
