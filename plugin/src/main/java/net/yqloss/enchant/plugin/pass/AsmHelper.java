package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsmHelper {
  public static MethodNode getOrCreateClassInitializer(ClassNode cn) {
    var init = cn.methods.stream().filter(it -> "<clinit>".equals(it.name)).findFirst().orElse(null);
    if (init == null) {
      cn.methods.add(init = new MethodNode(
        Opcodes.ACC_STATIC,
        "<clinit>",
        "()V",
        null,
        null
      ));
      init.instructions.add(new InsnNode(Opcodes.RETURN));
    }
    return init;
  }

  public static String findUniqueName(Stream<String> names, String prefix) {
    var possibleCollisions = names.filter(it -> it.startsWith(prefix)).collect(Collectors.toSet());
    for (var i = 0L; ; i++) {
      var name = prefix + i;
      if (possibleCollisions.contains(name)) continue;
      return name;
    }
  }

  public static void createInternalField(ClassNode cn, String name, String desc, Consumer<Consumer<AbstractInsnNode>> initializer) {
    cn.fields.add(new FieldNode(
      Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_TRANSIENT,
      name, desc, null, null
    ));
    var init = getOrCreateClassInitializer(cn);
    var list = new InsnList();
    initializer.accept(list::add);
    list.add(new FieldInsnNode(
      Opcodes.PUTSTATIC,
      cn.name,
      name,
      desc
    ));
    init.instructions.insert(list);
  }

  public static AbstractInsnNode nextExecutable(AbstractInsnNode node) {
    do {
      node = node.getNext();
    } while (node != null && node.getOpcode() <= 0);
    return node;
  }

  public static AbstractInsnNode instructionToExecute(AbstractInsnNode node) {
    while (node != null && node.getOpcode() <= 0) {
      node = node.getNext();
    }
    return node;
  }

  public static AbstractInsnNode previousExecutable(AbstractInsnNode node) {
    do {
      node = node.getPrevious();
    } while (node != null && node.getOpcode() <= 0);
    return node;
  }

  public static int getStackSize(Frame<?> frame) {
    return frame == null ? -1 : frame.getStackSize();
  }

  public static void debugMethod(MethodNode mn) {
    var textifier = new Textifier();
    var tmv = new TraceMethodVisitor(textifier);
    mn.accept(tmv);
    var pw = new PrintWriter(System.out);
    textifier.print(pw);
    pw.flush();
  }
}
