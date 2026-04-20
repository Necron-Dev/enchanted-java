package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ElvisPass implements Pass {
  Instance;

  private final Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());

  private final Frame<BasicValue> emptyFrame = new Frame<>(0, 0);

  private record AnalyzedInsn(
    AbstractInsnNode insn,
    Frame<BasicValue> frame
  ) {
  }

  private record Range(
    int start,
    int end
  ) {
  }

  @Override
  public boolean accept(ClassNode cn) {
    var modified = false;
    for (var mn : cn.methods) {
      var th = new ThrowHelper("elvis", cn, mn);

      try {
        var modifiedMethod = false;
        var frames = analyzer.analyze(cn.name, mn);
        var frameIterator = Arrays.stream(frames).iterator();
        var analyzed = new ArrayList<AnalyzedInsn>(frames.length);
        mn.instructions.forEach(insn -> {
          analyzed.add(new AnalyzedInsn(insn, frameIterator.next()));
        });

        for (var i = analyzed.size() - 1; i >= 0; i--) {
          var item = analyzed.get(i);

          if (item.insn instanceof MethodInsnNode min
            && min.getOpcode() == Opcodes.INVOKESTATIC
            && Enchanter.EnchantedJavaClasses.contains(min.owner)
            && "_elvis".equals(min.name)
          ) {
            modified = true;
            modifiedMethod = true;
            var label = new LabelNode();
            analyzed.set(i, new AnalyzedInsn(label, item.frame));

            if (AsmHelper.previousExecutable(item.insn) instanceof InsnNode in
              && (in.getOpcode() == Opcodes.AASTORE || in.getOpcode() == Opcodes.ANEWARRAY)
            ) {
              if (in.getOpcode() == Opcodes.ANEWARRAY) {
                throw th.raise("it is not allowed to invoke elvis without arguments; replace it with null");
              }

              var depth = item.frame.getStackSize();
              var pointer = i;
              var args = new ArrayList<Range>();

              outer:
              for (; ; ) {
                var j = pointer - 1;
                for (; j >= 0; j--) {
                  if (depth == analyzed.get(j).frame.getStackSize()) {
                    j--;
                    while (j >= 0 && analyzed.get(j).insn.getOpcode() <= 0)
                      j--;
                    j++;
                    var jtem = analyzed.get(j);

                    args.add(new Range(j, pointer));
                    pointer = j;

                    if (AsmHelper.instructionToExecute(jtem.insn) instanceof InsnNode in2
                      && in2.getOpcode() == Opcodes.DUP
                    ) {
                      if (j == 0) {
                        throw th.raise("the method body is not expected to start with DUP");
                      }

                      var prevInsn = analyzed.get(j - 1).insn;

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
                if (!first) {
                  var list = new InsnList();
                  list.add(new InsnNode(Opcodes.DUP));
                  list.add(new JumpInsnNode(Opcodes.IFNONNULL, label));
                  list.add(new InsnNode(Opcodes.POP));
                  i += insertInstructions(analyzed, arg.end, list);
                }
                first = false;
                i -= removePreviousNInstructions(analyzed, arg.end, 1);
                i -= removeNextNInstructions(analyzed, arg.start, 2);
              }

              i -= removePreviousNInstructions(analyzed, pointer, 2);
              continue;
            }

            throw th.raise("unpacking arrays is not allowed");
          }
        }

        if (modifiedMethod) {
          mn.instructions.clear();
          analyzed.forEach(item -> mn.instructions.add(item.insn));
        }
      } catch (AnalyzerException e) {
        throw th.raise(e, "failed to analyze method");
      }
    }

    return modified;
  }

  private int insertInstructions(List<AnalyzedInsn> list, int index, InsnList insnList) {
    list.addAll(index, Arrays.stream(insnList.toArray()).map(it -> new AnalyzedInsn(it, emptyFrame)).toList());
    return insnList.size();
  }

  private int removeNextNInstructions(List<AnalyzedInsn> list, int start, int n) {
    var remaining = n;
    for (var i = start; ; i++) {
      if (list.get(i).insn.getOpcode() > 0) {
        list.remove(i);
        i--;
        remaining--;
        if (remaining == 0) return n;
      }
    }
  }

  private int removePreviousNInstructions(List<AnalyzedInsn> list, int start, int n) {
    var remaining = n;
    for (var i = start - 1; ; i--) {
      if (list.get(i).insn.getOpcode() > 0) {
        list.remove(i);
        remaining--;
        if (remaining == 0) return n;
      }
    }
  }
}

