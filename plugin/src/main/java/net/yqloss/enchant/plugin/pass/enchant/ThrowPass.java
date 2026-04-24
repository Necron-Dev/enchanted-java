package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public enum ThrowPass implements Pass {
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
          && switch (min.name) {
            case "_throw", "_throw_", "$throw" -> true;
            default -> false;
          }
        ) {
          var nullable = "$throw".equals(min.name);
          var label = new LabelNode();
          modified = true;
          iter.remove();
          if (nullable) {
            iter.add(new InsnNode(Opcodes.DUP));
            iter.add(new JumpInsnNode(Opcodes.IFNULL, label));
          }
          iter.add(new InsnNode(Opcodes.ATHROW));
          if (nullable) {
            iter.add(label);
          }
        }
      }
    }
    return modified;
  }
}
