package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

public enum CachedPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var initializers = new LinkedHashMap<String, Consumer<Consumer<AbstractInsnNode>>>();
    var modified = false;
    for (var mn : cn.methods) {
      var th = new ThrowHelper("cached", cn, mn);
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && AsmHelper.isCallHook(min, "_cached")
        ) {
          modified = true;
          iter.remove();
          if (
            iter.previous() instanceof InvokeDynamicInsnNode idin
            && idin.bsmArgs[1] instanceof Handle handle
            && handle.getTag() == Opcodes.H_INVOKESTATIC
            && handle.getDesc().matches("\\(\\).*")
          ) {
            iter.remove();
            var field = AsmHelper.findUniqueName(
              cn.fields.stream().map(x -> x.name),
              "$$enchantedJava$cached"
            );
            cn.fields.add(new FieldNode(0, field, null, null, null));
            iter.add(new FieldInsnNode(
              Opcodes.GETSTATIC,
              cn.name,
              field,
              "Ljava/lang/Object;"
            ));
            initializers.put(
              field, emit -> {
                emit.accept(idin.clone(null));
                emit.accept(new MethodInsnNode(
                  Opcodes.INVOKEINTERFACE,
                  "java/util/function/Supplier",
                  "get",
                  "()Ljava/lang/Object;"
                ));
              }
            );
          } else {
            throw th.raise("_cached can only be used on a lambda without captures");
          }
        }
      }
    }
    for (var init : initializers.entrySet()) {
      AsmHelper.createInternalField(
        cn,
        init.getKey(),
        "Ljava/lang/Object;",
        init.getValue()
      );
    }
    return modified;
  }
}
