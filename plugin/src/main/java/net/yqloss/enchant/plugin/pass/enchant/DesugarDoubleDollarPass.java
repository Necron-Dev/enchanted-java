package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public enum DesugarDoubleDollarPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && AsmHelper.isCallHook(
            min,
            "$$return", "$$throw", "$$with", "$$also"
          )
        ) {
          modified = true;
          iter.remove();
          min.name = min.name.substring(1);
          AbstractInsnNode prev = null;
          if ("$with".equals(min.name) || "$also".equals(min.name)) {
            prev = iter.previous();
            iter.remove();
          }
          iter.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "yqloss/E",
            "$safe",
            "(Ljava/lang/Object;)Ljava/lang/Object;"
          ));
          if ("$throw".equals(min.name)) {
            iter.add(new TypeInsnNode(
              Opcodes.CHECKCAST,
              "java/lang/Throwable"
            ));
          }
          if (prev != null) {
            iter.add(prev);
          }
          iter.add(min);
        }
      }
    }
    return modified;
  }
}
