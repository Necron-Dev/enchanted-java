package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public enum ScopeFunctionPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    for (var mn : cn.methods) {
      var th = new ThrowHelper("scope-function", cn, mn);
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && min.getOpcode() == Opcodes.INVOKESTATIC
          && Enchanter.EnchantedJavaClasses.contains(min.owner)
          && switch (min.name) {
            case "_void", "_run", "_also", "_with", "$also", "$with" -> true;
            default -> false;
          }
        ) {
          modified = true;
          iter.remove();
          switch (min.name) {
            case "_void" -> {
              iter.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/lang/Runnable",
                "run",
                "()V",
                true
              ));
              iter.add(new InsnNode(Opcodes.ACONST_NULL));
            }

            case "_run" -> iter.add(new MethodInsnNode(
              Opcodes.INVOKEINTERFACE,
              "java/util/function/Supplier",
              "get",
              "()Ljava/lang/Object;",
              true
            ));

            case "_also" -> {
              iter.add(new InsnNode(Opcodes.SWAP));
              iter.add(new InsnNode(Opcodes.DUP_X1));
              iter.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/function/Consumer",
                "accept",
                "(Ljava/lang/Object;)V",
                true
              ));
            }

            case "_with" -> {
              iter.add(new InsnNode(Opcodes.SWAP));
              iter.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/function/Function",
                "apply",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                true
              ));
            }

            case "$also" -> {
              var label1 = new LabelNode();
              var label2 = new LabelNode();
              iter.add(new InsnNode(Opcodes.SWAP));
              iter.add(new InsnNode(Opcodes.DUP));
              iter.add(new JumpInsnNode(Opcodes.IFNULL, label1));
              iter.add(new InsnNode(Opcodes.DUP_X1));
              iter.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/function/Consumer",
                "accept",
                "(Ljava/lang/Object;)V",
                true
              ));
              iter.add(new JumpInsnNode(Opcodes.GOTO, label2));
              iter.add(label1);
              iter.add(new InsnNode(Opcodes.POP2));
              iter.add(new InsnNode(Opcodes.ACONST_NULL));
              iter.add(label2);
            }

            case "$with" -> {
              var label1 = new LabelNode();
              var label2 = new LabelNode();
              iter.add(new InsnNode(Opcodes.SWAP));
              iter.add(new InsnNode(Opcodes.DUP));
              iter.add(new JumpInsnNode(Opcodes.IFNULL, label1));
              iter.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/function/Function",
                "apply",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                true
              ));
              iter.add(new JumpInsnNode(Opcodes.GOTO, label2));
              iter.add(label1);
              iter.add(new InsnNode(Opcodes.POP2));
              iter.add(new InsnNode(Opcodes.ACONST_NULL));
              iter.add(label2);
            }

            default -> throw th.raise(
              "unknown scope function %s%s",
              min.name, min.desc
            );
          }
        }
      }
    }
    return modified;
  }
}
