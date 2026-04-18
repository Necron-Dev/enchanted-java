package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public enum NeverUnreachablePass implements Pass {
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
          && ("_never".equals(min.name) || "_unreachable".equals(min.name))
        ) {
          modified = true;
          iter.remove();
          iter.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalStateException"));
          iter.add(new InsnNode(Opcodes.DUP));
          iter.add(new LdcInsnNode("How did this even happen???"));
          iter.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/lang/IllegalStateException",
            "<init>",
            "(Ljava/lang/String;)V",
            false
          ));
          iter.add(new InsnNode(Opcodes.ATHROW));
        }
      }
    }
    return modified;
  }
}

