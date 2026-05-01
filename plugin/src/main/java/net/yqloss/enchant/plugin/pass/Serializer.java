package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.function.Consumer;

@FunctionalInterface
public interface Serializer {
  void serialize(Object object, Consumer<AbstractInsnNode> emit);

  Serializer PRIMITIVE_SERIALIZER = (object, emit) -> {
    object = object instanceof Boolean b ? (b ? 1 : 0) :
             object instanceof Byte b ? b.intValue() :
             object instanceof Short s ? s.intValue() :
             object instanceof Character c ? (int) c :
             object;
    if (object instanceof Integer i) {
      emit.accept(
        -1 <= i && i <= 5 ? new InsnNode(Opcodes.ICONST_0 + i) :
        Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE ? new IntInsnNode(Opcodes.BIPUSH, i) :
        Short.MIN_VALUE <= i && i <= Short.MAX_VALUE ? new IntInsnNode(Opcodes.SIPUSH, i) :
        new LdcInsnNode(object)
      );
    } else {
      emit.accept(new LdcInsnNode(object));
    }
  };

  Serializer BOXED_SERIALIZER = (object, emit) -> {
    try {
      PRIMITIVE_SERIALIZER.serialize(object, emit);
      var clazz = object.getClass();
      var primitive = (Class<?>) clazz.getDeclaredField("TYPE").get(null);
      emit.accept(new MethodInsnNode(
        Opcodes.INVOKESTATIC,
        clazz.getName().replace('.', '/'),
        "valueOf",
        String.format("(%s)%s", primitive.descriptorString(), clazz.descriptorString())
      ));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  };

  static Serializer primitiveArraySerializer(int arrayType) {
    return (object, emit) -> {
      try {
        var clazz = object.getClass();
        var elementType = Type.getType(clazz.componentType());
        var length = Array.getLength(object);
        PRIMITIVE_SERIALIZER.serialize(length, emit);
        emit.accept(new IntInsnNode(Opcodes.NEWARRAY, arrayType));
        for (var i = 0; i < length; i++) {
          emit.accept(new InsnNode(Opcodes.DUP));
          PRIMITIVE_SERIALIZER.serialize(i, emit);
          PRIMITIVE_SERIALIZER.serialize(Array.get(object, i), emit);
          emit.accept(new InsnNode(elementType.getOpcode(Opcodes.IASTORE)));
        }
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    };
  }

  Serializer STRING_SERIALIZER = (object, emit) -> {
    var string = (String) object;
    if (string.length() <= 32768) {
      emit.accept(new LdcInsnNode(string));
      return;
    }
    var chunks = new ArrayList<String>();
    for (var i = 0; i < string.length(); i += 32768) {
      chunks.add(string.substring(i, Math.min(i + 32768, string.length())));
    }
    emit.accept(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
    emit.accept(new InsnNode(Opcodes.DUP));
    PRIMITIVE_SERIALIZER.serialize(string.length(), emit);
    emit.accept(new MethodInsnNode(
      Opcodes.INVOKESPECIAL,
      "java/lang/StringBuilder",
      "<init>",
      "(I)V",
      false
    ));
    for (var chunk : chunks) {
      emit.accept(new LdcInsnNode(chunk));
      emit.accept(new MethodInsnNode(
        Opcodes.INVOKEVIRTUAL,
        "java/lang/StringBuilder",
        "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;"
      ));
    }
    emit.accept(new MethodInsnNode(
      Opcodes.INVOKEVIRTUAL,
      "java/lang/StringBuilder",
      "toString",
      "()Ljava/lang/String;"
    ));
  };

  Serializer BYTES_SERIALIZER = (object, emit) -> {
    try {
      var bytes = (byte[]) object;
      var encoded = Base64.getEncoder().encodeToString(bytes);
      emit.accept(new MethodInsnNode(
        Opcodes.INVOKESTATIC,
        "java/util/Base64",
        "getDecoder",
        "()Ljava/util/Base64$Decoder;"
      ));
      STRING_SERIALIZER.serialize(encoded, emit);
      emit.accept(new MethodInsnNode(
        Opcodes.INVOKEVIRTUAL,
        "java/util/Base64$Decoder",
        "decode",
        "(Ljava/lang/String;)[B"
      ));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  };

  Map<Class<?>, Serializer> SERIALIZERS = Map.ofEntries(
    Map.entry(
      Void.class,
      (object, emit) -> emit.accept(new InsnNode(Opcodes.ACONST_NULL))
    ),
    Map.entry(Boolean.class, BOXED_SERIALIZER),
    Map.entry(Byte.class, BOXED_SERIALIZER),
    Map.entry(Short.class, BOXED_SERIALIZER),
    Map.entry(Integer.class, BOXED_SERIALIZER),
    Map.entry(Long.class, BOXED_SERIALIZER),
    Map.entry(Float.class, BOXED_SERIALIZER),
    Map.entry(Double.class, BOXED_SERIALIZER),
    Map.entry(Character.class, BOXED_SERIALIZER),
    Map.entry(boolean[].class, primitiveArraySerializer(Opcodes.T_BOOLEAN)),
    Map.entry(byte[].class, BYTES_SERIALIZER),
    Map.entry(short[].class, primitiveArraySerializer(Opcodes.T_SHORT)),
    Map.entry(int[].class, primitiveArraySerializer(Opcodes.T_INT)),
    Map.entry(long[].class, primitiveArraySerializer(Opcodes.T_LONG)),
    Map.entry(float[].class, primitiveArraySerializer(Opcodes.T_FLOAT)),
    Map.entry(double[].class, primitiveArraySerializer(Opcodes.T_DOUBLE)),
    Map.entry(char[].class, primitiveArraySerializer(Opcodes.T_CHAR)),
    Map.entry(String.class, STRING_SERIALIZER)
  );

  Serializer INSTANCE = (object, emit) -> {
    var clazz = object == null ? Void.class : object.getClass();
    var registered = SERIALIZERS.get(clazz);
    if (registered != null) {
      registered.serialize(object, emit);
    } else if (clazz.isArray()) {
      try {
        var array = (Object[]) object;
        var elementType = Type.getType(clazz.componentType());
        var length = Array.getLength(array);
        PRIMITIVE_SERIALIZER.serialize(length, emit);
        emit.accept(new TypeInsnNode(Opcodes.ANEWARRAY, elementType.getInternalName()));
        for (var i = 0; i < length; i++) {
          emit.accept(new InsnNode(Opcodes.DUP));
          PRIMITIVE_SERIALIZER.serialize(i, emit);
          Serializer.INSTANCE.serialize(array[i], emit);
          emit.accept(new InsnNode(Opcodes.AASTORE));
        }
      } catch (Exception exception) {
        throw new UnsupportedOperationException("unsupported compile-time constant type: " + clazz);
      }
    } else {
      throw new UnsupportedOperationException("unsupported compile-time constant type: " + clazz);
    }
  };
}
