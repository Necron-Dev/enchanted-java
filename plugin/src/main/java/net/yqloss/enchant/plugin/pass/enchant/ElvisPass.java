package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.Analyzed;
import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public enum ElvisPass implements Pass {
  Instance;

  private record Range(
    int start,
    int end
  ) {}

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;

    for (var mn : cn.methods) {
      var th = new ThrowHelper("elvis", cn, mn);

      modified |= Analyzed.analyzed(
        th, cn, mn,
        analyzed -> {
          var modifiedMethod = false;
          for (var i = analyzed.size() - 1; i >= 0; i--) {
            var item = analyzed.get(i);

            if (
              item.insn() instanceof MethodInsnNode min
              && AsmHelper.isCallHook(min, "$elvis", "$([?])->?", "$$")
            ) {
              modifiedMethod = true;
              var isSafe = "$$".equals(min.name);
              var label = new LabelNode();
              analyzed.set(i, new Analyzed.InsnFrame(label, item.frame()));

              if (AsmHelper.previousExecutable(item.insn()) instanceof InsnNode in
                  && (in.getOpcode() == Opcodes.AASTORE || in.getOpcode() == Opcodes.ANEWARRAY)
              ) {
                if (in.getOpcode() == Opcodes.ANEWARRAY) {
                  throw th.raise("it is not allowed to invoke elvis without arguments; replace it with null");
                }

                var depth = AsmHelper.getStackSize(item.frame());
                if (depth == -1) continue;
                var pointer = i;
                var args = new ArrayList<Range>();

                outer:
                for (; ; ) {
                  var j = pointer - 1;
                  for (; j >= 0; j--) {
                    if (depth == AsmHelper.getStackSize(analyzed.get(j).frame())) {
                      do j--;
                      while (j >= 0 && analyzed.get(j).insn().getOpcode() <= 0);
                      j++;
                      var jtem = analyzed.get(j);

                      args.add(new Range(j, pointer));
                      pointer = j;

                      if (AsmHelper.instructionToExecute(jtem.insn()) instanceof InsnNode in2
                          && in2.getOpcode() == Opcodes.DUP
                      ) {
                        if (j == 0) {
                          throw th.raise("the method body is not expected to start with DUP");
                        }

                        var prevInsn = analyzed.get(j - 1).insn();

                        if (prevInsn.getOpcode() == Opcodes.ANEWARRAY) {
                          break outer;
                        } else if (prevInsn.getOpcode() == Opcodes.AASTORE) {
                          continue outer;
                        } else {
                          throw th.raise("the bytecode before DUP is neither ANEWARRAY nor AASTORE");
                        }
                      } else {
                        throw th.raise("the bytecode at the same depth is expected to be DUP");
                      }
                    }
                  }

                  throw th.raise("failed to locate a bytecode at the same depth");
                }

                var first = true;

                for (var arg : args) {
                  var list = new InsnList();
                  if (isSafe) {
                    list.add(new MethodInsnNode(
                      Opcodes.INVOKESTATIC,
                      "yqloss/E",
                      "$safe",
                      "(Ljava/lang/Object;)Ljava/lang/Object;"
                    ));
                  }
                  if (!first) {
                    list.add(new InsnNode(Opcodes.DUP));
                    list.add(new JumpInsnNode(Opcodes.IFNONNULL, label));
                    list.add(new InsnNode(Opcodes.POP));
                  }
                  i += Analyzed.insert(analyzed, arg.end, list);
                  first = false;
                  i -= Analyzed.removePrev(analyzed, arg.end, 1);
                  i -= Analyzed.removeNext(analyzed, arg.start, 2);
                }

                i -= Analyzed.removePrev(analyzed, pointer, 2);
                continue;
              }

              throw th.raise("unpacking arrays is not allowed");
            }
          }
          return modifiedMethod;
        }
      );
    }

    return modified;
  }
}

