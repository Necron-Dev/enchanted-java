package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public enum DesugarSingleDollarPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && AsmHelper.isCallHook(min, "$()->V")
        ) {
          modified = true;
          iter.remove();
          iter.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "yqloss/E",
            "safe_skipLast",
            "()Ljava/lang/Object;"
          ));
          iter.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "yqloss/E",
            "$safe",
            "(Ljava/lang/Object;)Ljava/lang/Object;"
          ));
          iter.add(new InsnNode(Opcodes.POP));
        }
      }
    }
    return modified;
  }
}
