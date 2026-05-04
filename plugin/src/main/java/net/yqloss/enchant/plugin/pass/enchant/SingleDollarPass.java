package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;

public enum SingleDollarPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof FieldInsnNode fin
          && AsmHelper.isSetHook(fin, "$")
        ) {
          modified = true;
          iter.set(new InsnNode(Opcodes.POP));
        }
      }
    }
    return modified;
  }
}
