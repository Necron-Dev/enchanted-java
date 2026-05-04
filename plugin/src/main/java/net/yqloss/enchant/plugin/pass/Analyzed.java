package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class Analyzed {
  public record InsnFrame(
    AbstractInsnNode insn,
    Frame<BasicValue> frame
  ) {}

  public static boolean analyzed(ThrowHelper th, ClassNode cn, MethodNode mn, Predicate<List<InsnFrame>> handler) {
    var analyzer = new Analyzer<>(new BasicInterpreter());
    mn.maxStack = 65535;
    try {
      var frames = analyzer.analyze(cn.name, mn);
      var frameIterator = Arrays.stream(frames).iterator();
      var analyzed = new ArrayList<InsnFrame>(frames.length);
      mn.instructions.forEach(insn -> {
        analyzed.add(new InsnFrame(insn, frameIterator.next()));
      });
      if (handler.test(analyzed)) {
        mn.instructions.clear();
        analyzed.forEach(item -> mn.instructions.add(item.insn));
        return true;
      }
      return false;
    } catch (AnalyzerException e) {
      throw th.raise(e, "failed to analyze method");
    }
  }

  public static int insert(List<InsnFrame> list, int index, InsnList insnList) {
    list.addAll(
      index,
      Arrays
        .stream(insnList.toArray())
        .map(it -> new InsnFrame(it, new Frame<>(0, 0)))
        .toList()
    );
    return insnList.size();
  }

  public static int removeNext(List<InsnFrame> list, int start, int n) {
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

  public static int removePrev(List<InsnFrame> list, int start, int n) {
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
