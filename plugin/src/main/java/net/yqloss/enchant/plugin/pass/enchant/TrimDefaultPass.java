package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.Analyzed;
import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

public enum TrimDefaultPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;

    for (var mn : cn.methods) {
      var th = new ThrowHelper("trim-default", cn, mn);

      modified |= Analyzed.analyzed(
        th, cn, mn,
        analyzed -> {
          var modifiedMethod = false;
          for (var i = analyzed.size() - 1; i >= 0; i--) {
            var item = analyzed.get(i);

            if (
              item.insn() instanceof MethodInsnNode min
              && AsmHelper.isCallHook(min, "_default")
            ) {
              modifiedMethod = true;

              var depth = AsmHelper.getStackSize(analyzed.get(i + 1).frame());
              if (depth == -1) continue;

              var j = i;
              for (; j >= 0; j--) {
                var jtem = analyzed.get(j);
                var jDepth = AsmHelper.getStackSize(jtem.frame());
                analyzed.remove(j);
                if (jDepth <= depth) break;
              }
              i = j;
            }
          }
          return modifiedMethod;
        }
      );
    }

    return modified;
  }
}
