package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.function.Consumer;

public class TypeConverter {
  public static void convert(ThrowHelper th, Consumer<AbstractInsnNode> append, Type fromType, Type toType) {
    var fromSort = fromType.getSort() == Type.ARRAY ? Type.OBJECT : fromType.getSort();
    var toSort = toType.getSort() == Type.ARRAY ? Type.OBJECT : toType.getSort();

    if (fromSort == Type.METHOD || toSort == Type.METHOD) {
      throw th.raise("convert does not work on method types");
    }

    if (fromType.equals(toType)) return;

    if (fromSort == toSort) {
      if (fromSort == Type.OBJECT) {
        append.accept(new TypeInsnNode(Opcodes.CHECKCAST, toType.getInternalName()));
      }
      return;
    }

    if (fromSort == Type.VOID) {
      throw th.raise("cannot convert void to non-void types");
    }

    if (toSort == Type.VOID) {
      append.accept(new InsnNode(fromType.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
    }

    if (fromSort == Type.OBJECT) {
      switch (toSort) {
        case Type.BOOLEAN ->
          fromObject(append, toType, Boolean.class, "booleanValue");
        case Type.CHAR ->
          fromObject(append, toType, Character.class, "charValue");
        case Type.BYTE -> fromObject(append, toType, Byte.class, "byteValue");
        case Type.SHORT ->
          fromObject(append, toType, Short.class, "shortValue");
        case Type.INT -> fromObject(append, toType, Integer.class, "intValue");
        case Type.FLOAT ->
          fromObject(append, toType, Float.class, "floatValue");
        case Type.LONG -> fromObject(append, toType, Long.class, "longValue");
        case Type.DOUBLE ->
          fromObject(append, toType, Double.class, "doubleValue");
      }
      return;
    }

    if (toSort == Type.OBJECT) {
      switch (fromSort) {
        case Type.BOOLEAN -> toObject(append, toType, Boolean.class);
        case Type.CHAR -> toObject(append, toType, Character.class);
        case Type.BYTE -> toObject(append, toType, Byte.class);
        case Type.SHORT -> toObject(append, toType, Short.class);
        case Type.INT -> toObject(append, toType, Integer.class);
        case Type.FLOAT -> toObject(append, toType, Float.class);
        case Type.LONG -> toObject(append, toType, Long.class);
        case Type.DOUBLE -> toObject(append, toType, Double.class);
      }
      return;
    }

    throw th.raise("cannot convert between primitive types");
  }

  private static void fromObject(Consumer<AbstractInsnNode> append, Type primitive, Class<?> clazz, String method) {
    var type = Type.getType(clazz);
    append.accept(new TypeInsnNode(Opcodes.CHECKCAST, type.getInternalName()));
    append.accept(new MethodInsnNode(
      Opcodes.INVOKEVIRTUAL,
      type.getInternalName(),
      method,
      "()" + primitive.getDescriptor(),
      false
    ));
  }

  private static void toObject(Consumer<AbstractInsnNode> append, Type primitive, Class<?> clazz) {
    var type = Type.getType(clazz);
    append.accept(new MethodInsnNode(
      Opcodes.INVOKESTATIC,
      type.getInternalName(),
      "valueOf",
      "(" + primitive.getDescriptor() + ")" + type.getDescriptor(),
      false
    ));
  }

  public static String extractParameters(String methodDesc) {
    return methodDesc.substring(methodDesc.indexOf('(') + 1, methodDesc.indexOf(')'));
  }
}
