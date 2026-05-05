package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ErrorTable {
  private record Error(LabelNode label, Runnable add) {}

  private final List<Error> errors = new ArrayList<>();

  public LabelNode add(Consumer<LabelNode> add) {
    var label = new LabelNode();
    errors.add(new Error(label, () -> add.accept(label)));
    return label;
  }

  public LabelNode add(Class<?> exception, Consumer<AbstractInsnNode> accept, Runnable prologue, Runnable addMessage) {
    var name = exception.getName().replace('.', '/');
    return add(label -> {
      accept.accept(label);
      prologue.run();
      accept.accept(new TypeInsnNode(Opcodes.NEW, name));
      accept.accept(new InsnNode(Opcodes.DUP));
      addMessage.run();
      accept.accept(new MethodInsnNode(
        Opcodes.INVOKESPECIAL,
        name,
        "<init>",
        "(Ljava/lang/String;)V"
      ));
      accept.accept(new InsnNode(Opcodes.ATHROW));
    });
  }

  public LabelNode add(Class<?> exception, Consumer<AbstractInsnNode> accept, Runnable addMessage) {
    return add(exception, accept, () -> {}, addMessage);
  }

  public void insertAll() {
    errors.forEach(x -> x.add.run());
  }

  public void insert(LabelNode label) {
    errors.stream()
      .filter(x -> x.label == label)
      .forEach(x -> x.add.run());
    errors.removeIf(x -> x.label == label);
  }
}
