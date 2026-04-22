package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum SafePass implements Pass {
  Instance;

  private record AnalyzedInsn(
    AbstractInsnNode insn,
    Frame<BasicValue> frame
  ) {
  }

  @Override
  public boolean accept(ClassNode cn) {
    var modified = false;
    var analyzer = new Analyzer<>(new BasicInterpreter());

    for (var mn : cn.methods) {
      var th = new ThrowHelper("safe", cn, mn);

      try {
        var modifiedMethod = false;
        mn.maxStack = 65535;
        var frames = analyzer.analyze(cn.name, mn);
        var frameIterator = Arrays.stream(frames).iterator();
        var analyzed = new ArrayList<AnalyzedInsn>(frames.length);
        mn.instructions.forEach(insn -> {
          analyzed.add(new AnalyzedInsn(insn, frameIterator.next()));
        });

        for (var i = analyzed.size() - 1; i >= 0; i--) {
          var item = analyzed.get(i);

          if (
            item.insn instanceof MethodInsnNode min
            && min.getOpcode() == Opcodes.INVOKESTATIC
            && Enchanter.EnchantedJavaClasses.contains(min.owner)
            && switch (min.name) {
              case "$safe" -> true;
              case "$" ->
                "(Ljava/lang/Object;)Ljava/lang/Object;".equals(min.desc);
              default -> false;
            }
          ) {
            modified = true;
            modifiedMethod = true;
            var label = new LabelNode();
            analyzed.set(i, new AnalyzedInsn(label, item.frame));

            var depth = item.frame.getStackSize();
            var operations = new ArrayList<Integer>();

            for (var j = i; j >= 0; j--) {
              var jtem = analyzed.get(j);
              var jDepth = jtem.frame.getStackSize();
              if (jDepth < depth) break;

              if (depth == jDepth) {
                do j--;
                while (j >= 0 && analyzed.get(j).insn.getOpcode() <= 0);
                j++;
                if (jtem.frame.getStack(depth - 1).isReference()) {
                  operations.add(j);
                }
              }
            }

            for (var operation : operations) {
              var insn = analyzed.get(operation - 1).insn;

              if (insn.getOpcode() == Opcodes.CHECKCAST) {
                var type = ((TypeInsnNode) insn).desc;
                var list = new InsnList();
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new TypeInsnNode(Opcodes.INSTANCEOF, type));
                list.add(new JumpInsnNode(Opcodes.IFEQ, label));
                i += insertInstructions(analyzed, operation - 1, list);
              } else {
                var list = new InsnList();
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new JumpInsnNode(Opcodes.IFNULL, label));
                i += insertInstructions(analyzed, operation, list);

                if (
                  insn instanceof MethodInsnNode min2
                  && min2.getOpcode() == Opcodes.INVOKESTATIC
                  && Enchanter.EnchantedJavaClasses.contains(min2.owner)
                ) {
                  if (
                    switch (min2.name) {
                      case "$safe" -> true;
                      case "$" ->
                        "(Ljava/lang/Object;)Ljava/lang/Object;".equals(min.desc);
                      default -> false;
                    }
                  ) {
                    throw th.raise("$safe cannot be used directly in another $safe");
                  }

                  if ("$unsafe".equals(min2.name)) {
                    analyzed.remove(operation - 1);
                    i -= 1;
                    break;
                  }
                }
              }
            }
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
    list.addAll(
      index,
      Arrays
        .stream(insnList.toArray())
        .map(it -> new AnalyzedInsn(it, new Frame<>(0, 0)))
        .toList()
    );
    return insnList.size();
  }
}
