package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class TypeHelper {
  public static void convert(Consumer<AbstractInsnNode> append, Type fromType, Type toType) {
    var fromSort = fromType.getSort() == Type.ARRAY ? Type.OBJECT : fromType.getSort();
    var toSort = toType.getSort() == Type.ARRAY ? Type.OBJECT : toType.getSort();

    if (fromSort == Type.METHOD || toSort == Type.METHOD) {
      throw new UnsupportedOperationException("convert does not work on method types");
    }

    if (fromType.equals(toType)) return;

    if (fromSort == toSort) {
      if (fromSort == Type.OBJECT) {
        append.accept(new TypeInsnNode(Opcodes.CHECKCAST, toType.getInternalName()));
      }
      return;
    }

    if (fromSort == Type.VOID) {
      if (toSort == Type.OBJECT) {
        append.accept(new InsnNode(Opcodes.ACONST_NULL));
        return;
      }
      throw new UnsupportedOperationException("cannot convert void to non-void types");
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
        case Type.BOOLEAN -> toObject(append, fromType, Boolean.class);
        case Type.CHAR -> toObject(append, fromType, Character.class);
        case Type.BYTE -> toObject(append, fromType, Byte.class);
        case Type.SHORT -> toObject(append, fromType, Short.class);
        case Type.INT -> toObject(append, fromType, Integer.class);
        case Type.FLOAT -> toObject(append, fromType, Float.class);
        case Type.LONG -> toObject(append, fromType, Long.class);
        case Type.DOUBLE -> toObject(append, fromType, Double.class);
      }
      return;
    }

    throw new UnsupportedOperationException("cannot convert between primitive types");
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

  public static void zero(Consumer<AbstractInsnNode> append, Type type) {
    if (type.getSort() == Type.VOID) return;
    append.accept(new InsnNode(
      switch (type.getSort()) {
        case Type.OBJECT, Type.ARRAY -> Opcodes.ACONST_NULL;
        case Type.FLOAT -> Opcodes.FCONST_0;
        case Type.DOUBLE -> Opcodes.DCONST_0;
        case Type.LONG -> Opcodes.LCONST_0;
        default -> Opcodes.ICONST_0;
      }
    ));
  }

  public static void init(Consumer<AbstractInsnNode> append, Type type, int index) {
    if (type.getSort() == Type.VOID) return;
    zero(append, type);
    append.accept(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), index));
  }

  @SafeVarargs
  public static void buildString(
    Consumer<AbstractInsnNode> append,
    Function<Consumer<AbstractInsnNode>, Object>... segments
  ) {
    append.accept(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
    append.accept(new InsnNode(Opcodes.DUP));
    append.accept(new MethodInsnNode(
      Opcodes.INVOKESPECIAL,
      "java/lang/StringBuilder",
      "<init>",
      "()V"
    ));
    var stringType = Type.getType(String.class);
    for (var segment : segments) {
      var result = segment.apply(append);
      if (result instanceof Type type) {
        convert(append, type, stringType);
      } else if (result != null) {
        append.accept(new LdcInsnNode(result.toString()));
      }
      append.accept(new MethodInsnNode(
        Opcodes.INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;"
      ));
    }
    append.accept(new MethodInsnNode(
      Opcodes.INVOKEVIRTUAL,
      "java/lang/StringBuilder",
      "toString",
      "()Ljava/lang/String;"
    ));
  }

  public static void buildString(
    Consumer<AbstractInsnNode> append,
    BiConsumer<Consumer<AbstractInsnNode>, Consumer<Object>> segments
  ) {
    append.accept(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
    append.accept(new InsnNode(Opcodes.DUP));
    append.accept(new MethodInsnNode(
      Opcodes.INVOKESPECIAL,
      "java/lang/StringBuilder",
      "<init>",
      "()V"
    ));
    var stringType = Type.getType(String.class);
    segments.accept(
      append, result -> {
        if (result instanceof Type type) {
          convert(append, type, stringType);
        } else if (result != null) {
          append.accept(new LdcInsnNode(result.toString()));
        }
        append.accept(new MethodInsnNode(
          Opcodes.INVOKEVIRTUAL,
          "java/lang/StringBuilder",
          "append",
          "(Ljava/lang/String;)Ljava/lang/StringBuilder;"
        ));
      }
    );
    append.accept(new MethodInsnNode(
      Opcodes.INVOKEVIRTUAL,
      "java/lang/StringBuilder",
      "toString",
      "()Ljava/lang/String;"
    ));
  }
}
