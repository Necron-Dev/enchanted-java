package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public enum PassCastPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && AsmHelper.isCallHook(min, "_pass", "_cast")
        ) {
          modified = true;
          iter.remove();
        }
      }
    }
    return modified;
  }
}
