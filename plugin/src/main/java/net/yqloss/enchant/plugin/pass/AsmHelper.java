package net.yqloss.enchant.plugin.pass;

import net.yqloss.enchant.plugin.Enchanter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
    cn.fields.removeIf(x -> Objects.equals(x.name, name));
    cn.fields.add(new FieldNode(
      (cn.access & Opcodes.ACC_INTERFACE) == 0
      ? Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_TRANSIENT
      : Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
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
    System.out.println();
    var pw = new PrintWriter(System.out);
    textifier.print(pw);
    pw.flush();
    System.out.println();
  }

  public static void debugClass(ClassNode cn) {
    System.out.println();
    var pw = new PrintWriter(System.out);
    var tcv = new TraceClassVisitor(pw);
    cn.accept(tcv);
    pw.flush();
    System.out.println();
  }

  public static boolean isCallHook(MethodInsnNode min, String... methods) {
    if (
      min.getOpcode() == Opcodes.INVOKESTATIC
      && Enchanter.EnchantedJavaClasses.contains(min.owner)
    ) {
      for (var method : methods) {
        if (method.contains("(")) {
          method = method
                     .replace("?", "Ljava/lang/Object;")
                     .replace("->", "")
                     .replace("<", "L")
                     .replace(">", ";")
                     .replace("]", "")
                     .replace(" ", "")
                     .replace(",", "");
          if (Objects.equals(min.name + min.desc, method)) {
            return true;
          }
        } else {
          if (Objects.equals(min.name, method)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean isGetHook(FieldInsnNode fin, String... fields) {
    if (
      fin.getOpcode() == Opcodes.GETSTATIC
      && Enchanter.EnchantedJavaClasses.contains(fin.owner)
    ) {
      return Arrays.asList(fields).contains(fin.name);
    }
    return false;
  }

  public static boolean isSetHook(FieldInsnNode fin, String... fields) {
    if (
      fin.getOpcode() == Opcodes.PUTSTATIC
      && Enchanter.EnchantedJavaClasses.contains(fin.owner)
    ) {
      return Arrays.asList(fields).contains(fin.name);
    }
    return false;
  }

  public static boolean containsStub(InsnList list, String... methods) {
    for (var insn : list) {
      if (
        insn instanceof MethodInsnNode min
        && isCallHook(min, methods)
      ) {
        return true;
      }
    }
    return false;
  }

  public static Map<String, Object> getAnnotation(
    List<? extends AnnotationNode> annotations,
    String descriptor
  ) {
    if (annotations == null) return null;
    for (var annotation : annotations) {
      if (annotation.desc.equals(descriptor)) {
        var values = annotation.values;
        if (values == null) return Map.of("value", "");
        var map = new HashMap<String, Object>();
        for (var i = 0; i < values.size(); i += 2) {
          map.put((String) values.get(i), values.get(i + 1));
        }
        return map;
      }
    }
    return null;
  }

  public static <T> T safeGet(T[] array, int index) {
    if (array == null || index < 0 || index >= array.length) return null;
    return array[index];
  }

  public static <V, T> T fromAnnotation(
    List<? extends AnnotationNode> annotations,
    String descriptor,
    Function<? super V, ? extends T> mapper,
    Supplier<? extends T> defaultValue
  ) {
    var annotation = getAnnotation(annotations, descriptor);
    if (annotation == null) return defaultValue.get();
    var value = (V) annotation.get("value");
    return mapper.apply(value);
  }

  public static <T> T fromAnnotation(
    List<? extends AnnotationNode> annotations,
    String descriptor,
    Supplier<? extends T> defaultValue
  ) {
    var annotation = getAnnotation(annotations, descriptor);
    if (annotation == null) return defaultValue.get();
    var value = (T) annotation.get("value");
    if ("".equals(value)) return defaultValue.get();
    return value;
  }
}
