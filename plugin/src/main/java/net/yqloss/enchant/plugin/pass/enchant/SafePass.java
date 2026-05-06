package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.Analyzed;
import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public enum SafePass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;

    for (var mn : cn.methods) {
      var th = new ThrowHelper("safe", cn, mn);

      modified |= Analyzed.analyzed(
        th, cn, mn,
        analyzed -> {
          var modifiedMethod = false;
          for (var i = analyzed.size() - 1; i >= 0; i--) {
            var item = analyzed.get(i);

            if (
              item.insn() instanceof MethodInsnNode min
              && AsmHelper.isCallHook(min, "$safe", "$(?)->?")
            ) {
              modifiedMethod = true;
              var label = new LabelNode();
              var labelNotInstance = new LabelNode();
              analyzed.set(i, new Analyzed.InsnFrame(label, item.frame()));

              var depth = AsmHelper.getStackSize(item.frame());
              if (depth == -1) continue;
              var operations = new ArrayList<Integer>();
              var containsCheckCast = false;

              for (var j = i; j >= 0; j--) {
                var jtem = analyzed.get(j);
                var jDepth = AsmHelper.getStackSize(jtem.frame());
                if (jDepth < depth) {
                  if (
                    jtem.insn() instanceof MethodInsnNode min2
                    && AsmHelper.isCallHook(min2, "safe_skipLast")
                  ) {
                    var list = new InsnList();
                    list.add(new InsnNode(Opcodes.ACONST_NULL));
                    Analyzed.removeNext(analyzed, j, 1);
                    Analyzed.insert(analyzed, j, list);
                    do j--;
                    while (j >= 0 && analyzed.get(j).insn().getOpcode() <= 0);
                    j++;
                  } else {
                    break;
                  }
                }

                if (depth == jDepth) {
                  do j--;
                  while (j >= 0 && analyzed.get(j).insn().getOpcode() <= 0);
                  j++;
                  if (jtem.frame().getStack(depth - 1).isReference()) {
                    var lastOpcode = analyzed.get(j - 1).insn().getOpcode();
                    if (lastOpcode == Opcodes.CHECKCAST) {
                      containsCheckCast = true;
                    }
                    if (lastOpcode != Opcodes.NEW) {
                      operations.add(j);
                    }
                  }
                }
              }

              if (containsCheckCast) {
                var l = new InsnList();
                l.add(new JumpInsnNode(Opcodes.GOTO, label));
                l.add(labelNotInstance);
                l.add(new InsnNode(Opcodes.POP));
                l.add(new InsnNode(Opcodes.ACONST_NULL));
                Analyzed.insert(analyzed, i, l);
              }

              var first = true;

              loop:
              for (var operation : operations) {
                var insn = analyzed.get(operation - 1).insn();

                if (insn.getOpcode() == Opcodes.CHECKCAST) {
                  var type = ((TypeInsnNode) insn).desc;
                  var list = new InsnList();
                  list.add(new InsnNode(Opcodes.DUP));
                  list.add(new TypeInsnNode(Opcodes.INSTANCEOF, type));
                  list.add(new JumpInsnNode(Opcodes.IFEQ, labelNotInstance));
                  i += Analyzed.insert(analyzed, operation - 1, list);
                } else {
                  if (!first) {
                    var list = new InsnList();
                    list.add(new InsnNode(Opcodes.DUP));
                    list.add(new JumpInsnNode(Opcodes.IFNULL, label));
                    i += Analyzed.insert(analyzed, operation, list);
                  }

                  if (
                    insn instanceof MethodInsnNode min2
                    && AsmHelper.isCallHook(min2, "$safe", "$(?)->?", "$unsafe")
                  ) {
                    switch (min2.name) {
                      case "$safe", "$" -> {
                        throw th.raise("$safe cannot be used directly in another $safe");
                      }

                      case "$unsafe" -> {
                        analyzed.remove(operation - 1);
                        i -= 1;
                        break loop;
                      }

                      default -> {}
                    }
                  }
                }

                first = false;
              }
            }
          }
          return modifiedMethod;
        }
      );
    }

    return modified;
  }
}
