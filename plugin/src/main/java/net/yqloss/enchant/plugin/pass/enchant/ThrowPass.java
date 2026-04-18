package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public enum ThrowPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (iter.next() instanceof MethodInsnNode min
          && min.getOpcode() == Opcodes.INVOKESTATIC
          && Enchanter.EnchantedJavaClasses.contains(min.owner)
          && "_throw".equals(min.name)
        ) {
          modified = true;
          iter.remove();
          iter.add(new InsnNode(Opcodes.ATHROW));
        }
      }
    }
    return modified;
  }
}
