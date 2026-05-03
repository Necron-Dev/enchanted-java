package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import net.yqloss.enchant.plugin.pass.TypeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public enum DefaultArgsPass implements Pass {
  Instance;

  private record TypeAndName(
    Type type,
    String name
  ) {}

  private record ParameterInfo(
    int index,
    BigInteger bit,
    int stack,
    String name,
    Type type,
    DefaultValue defaultValue,
    LabelNode missingDependenciesError
  ) {
    int getHelper() {
      return index >> 5;
    }

    int getHelperBit() {
      return 1 << (index & 0x1F);
    }
  }

  @FunctionalInterface
  private interface DefaultValue {
    default List<TypeAndName> dependencies() {
      return List.of();
    }

    void compute(Consumer<AbstractInsnNode> accept, Map<String, ParameterInfo> map);
  }

  private record Branch(LabelNode label, List<ParameterInfo> params) {}

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    var objectType = Type.getType(Object.class);
    var argClass = AsmHelper.fromAnnotation(
      cn.invisibleAnnotations,
      "Lyqloss/E$ArgClass;",
      (Type t) -> t.getInternalName(),
      () -> cn.name + "$Arg"
    );

    for (var mn : cn.methods) {
      var th = new ThrowHelper("default-args", cn, mn);
      if (AsmHelper.containsStub(mn.instructions, "_defaultArgs")) {
        modified = true;
        var desc = Type.getMethodType(mn.desc);
        if (!(
          desc.getArgumentCount() == 1
          && (Object) desc.getArgumentTypes()[0] instanceof Type t
          && t.getSort() == Type.ARRAY
          && t.getElementType().getSort() == Type.OBJECT
          && argClass.equals(t.getElementType().getInternalName())
        )) {
          throw th.raise(
            "_defaultArgs methods must take exactly one argument with type %s[]",
            argClass
          );
        }
        var backing = AsmHelper.fromAnnotation(
          mn.invisibleAnnotations,
          "Lyqloss/E$Name;",
          () -> mn.name.replaceAll("[<>]", "")
        );
        MethodNode backingMethodMut = null;
        {
          var nonAnnotated = new ArrayList<MethodNode>();
          for (var mn2 : cn.methods) {
            if (mn == mn2) continue;
            var nameFromAnnotation = AsmHelper.fromAnnotation(
              mn2.invisibleAnnotations,
              "Lyqloss/E$Name;",
              () -> null
            );
            if (backing.equals(nameFromAnnotation)) {
              if (backingMethodMut != null) {
                throw th.raise(
                  "multiple methods annotated as %s are found",
                  backing
                );
              }
              backingMethodMut = mn2;
            } else if (backing.equals(mn2.name.replaceAll("[<>]", ""))) {
              nonAnnotated.add(mn2);
            }
          }
          if (backingMethodMut == null) {
            if (nonAnnotated.isEmpty()) {
              throw th.raise(
                "no method matching %s found",
                backing
              );
            } else if (nonAnnotated.size() > 1) {
              throw th.raise(
                "multiple methods named %s are found, try annotate the expected one with @Name(\"%s\")",
                backing, backing
              );
            } else {
              backingMethodMut = nonAnnotated.get(0);
            }
          }
        }
        var backingMethod = backingMethodMut;
        var backingDesc = Type.getMethodType(backingMethod.desc);
        if (
          (mn.access & Opcodes.ACC_STATIC) != (backingMethod.access & Opcodes.ACC_STATIC)
          || !desc.getReturnType().equals(backingDesc.getReturnType())
        ) {
          throw th.raise("methods must be both static or both non-static and return the same type");
        }
        var isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        var argsIndex = isStatic ? 0 : 1;
        var tempIndex = argsIndex + 1;
        mn.maxLocals = isStatic ? 2 : 3;
        mn.tryCatchBlocks.clear();
        var list = mn.instructions;
        list.clear();
        TypeHelper.init(list::add, objectType, tempIndex);
        var helperIntIndex = mn.maxLocals;
        var helperCount = (backingDesc.getArgumentCount() + 31) >> 5;
        mn.maxLocals += helperCount;
        for (var i = helperIntIndex; i < mn.maxLocals; i++) {
          TypeHelper.init(list::add, Type.INT_TYPE, i);
        }
        var params = new ArrayList<ParameterInfo>();
        var nameToParam = new HashMap<String, ParameterInfo>();

        for (var i = 0; i < backingDesc.getArgumentCount(); i++) {
          var finalI = i;
          var name = AsmHelper.fromAnnotation(
            AsmHelper.safeGet(backingMethod.invisibleParameterAnnotations, i),
            "Lyqloss/E$Name;",
            () -> getParamName(th, backingMethod, finalI)
          );
          var type = backingDesc.getArgumentTypes()[i];
          var param = new ParameterInfo(
            i,
            BigInteger.ONE.shiftLeft(i),
            mn.maxLocals,
            name,
            type,
            AsmHelper.fromAnnotation(
              AsmHelper.safeGet(backingMethod.invisibleParameterAnnotations, i),
              "Lyqloss/E$Default;",
              (String value) -> getDefaultValue(th, cn, type, backing, name, value, isStatic),
              () -> null
            ),
            new LabelNode()
          );
          params.add(param);
          nameToParam.put(name, param);
          TypeHelper.init(list::add, type, mn.maxLocals);
          mn.maxLocals += type.getSize();
        }

        for (var param : params) {
          if (param.defaultValue == null) continue;
          for (var dep : param.defaultValue.dependencies()) {
            if (!nameToParam.containsKey(dep.name)) {
              throw th.raise(
                "dependency %s for %s not found",
                dep.name, param.name
              );
            }
            var type = nameToParam.get(dep.name).type;
            if (!type.equals(dep.type)) {
              throw th.raise(
                "type of dependency %s for %s does not match parameter type",
                dep.name, param.name
              );
            }
          }
        }

        var loopLabel = new LabelNode();
        var breakLabel = new LabelNode();
        var entryNullLabel = new LabelNode();
        list.add(new InsnNode(Opcodes.ICONST_0));
        // [i]
        list.add(loopLabel);
        list.add(new VarInsnNode(Opcodes.ALOAD, argsIndex));
        // [i, args]
        list.add(new JumpInsnNode(Opcodes.IFNULL, breakLabel));
        // [i]

        // while (i <= args.length)
        list.add(new VarInsnNode(Opcodes.ALOAD, argsIndex));
        // [i, args]
        list.add(new InsnNode(Opcodes.ARRAYLENGTH));
        // [i, args.length]
        list.add(new InsnNode(Opcodes.SWAP));
        // [args.length, i]
        list.add(new InsnNode(Opcodes.DUP_X1));
        // [i, args.length, i]
        list.add(new JumpInsnNode(Opcodes.IF_ICMPLE, breakLabel));
        // [i]

        // switch (args[i].name) and leave a copy of args[i] and args[i].name
        list.add(new VarInsnNode(Opcodes.ALOAD, argsIndex));
        // [i, args]
        list.add(new InsnNode(Opcodes.SWAP));
        // [args, i]
        list.add(new InsnNode(Opcodes.DUP_X1));
        // [i, args, i]
        list.add(new InsnNode(Opcodes.AALOAD));
        // [i, args[i]]
        list.add(new InsnNode(Opcodes.DUP));
        // [i, args[i], args[i]]
        list.add(new JumpInsnNode(Opcodes.IFNULL, entryNullLabel));
        // [i, args[i]]
        list.add(new InsnNode(Opcodes.DUP));
        // [i, args[i], args[i]]
        list.add(new MethodInsnNode(
          Opcodes.INVOKEVIRTUAL,
          argClass,
          "name",
          "()Ljava/lang/String;"
        ));
        // [i, args[i], args[i].name]
        list.add(new InsnNode(Opcodes.DUP));
        // [i, args[i], args[i].name, args[i].name]

        // if (name == null) throw new IllegalArgumentException(...);
        var switchBegin = new LabelNode();
        list.add(new JumpInsnNode(Opcodes.IFNONNULL, switchBegin));
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalArgumentException"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new LdcInsnNode("argument name cannot be null"));
        list.add(new MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          "java/lang/IllegalArgumentException",
          "<init>",
          "(Ljava/lang/String;)V"
        ));
        list.add(new InsnNode(Opcodes.ATHROW));

        var alreadyFilledError = new LabelNode();
        list.add(alreadyFilledError);
        // [i, args[i], args[i].name, helper, helperBit]
        list.add(new InsnNode(Opcodes.POP2));
        // [i, args[i], args[i].name]
        list.add(new VarInsnNode(Opcodes.ASTORE, tempIndex));
        // [i, args[i]]
        // throw new IllegalArgumentException(...)
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalArgumentException"));
        list.add(new InsnNode(Opcodes.DUP));
        TypeHelper.buildString(
          list::add,
          l -> "argument already filled: ",
          l -> {
            l.accept(new VarInsnNode(Opcodes.ALOAD, tempIndex));
            return null;
          }
        );
        list.add(new MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          "java/lang/IllegalArgumentException",
          "<init>",
          "(Ljava/lang/String;)V"
        ));
        list.add(new InsnNode(Opcodes.ATHROW));

        list.add(switchBegin);
        // [i, args[i], args[i].name]
        list.add(new InsnNode(Opcodes.DUP));
        // [i, args[i], args[i].name, args[i].name]
        list.add(new MethodInsnNode(
          Opcodes.INVOKEVIRTUAL,
          "java/lang/String",
          "hashCode",
          "()I"
        ));
        // [i, args[i], args[i].name, args[i].name.hashCode()]

        var branches = new HashMap<Integer, Branch>();
        for (var param : params) {
          var hashCode = param.name.hashCode();
          if (!branches.containsKey(hashCode)) {
            branches.put(hashCode, new Branch(new LabelNode(), new ArrayList<>()));
          }
          branches.get(hashCode).params.add(param);
        }
        var branchEntries = branches.entrySet().stream()
                              .sorted(Map.Entry.comparingByKey())
                              .toList();

        var switchEnd = new LabelNode();
        var switchDefault = new LabelNode();
        list.add(new LookupSwitchInsnNode(
          switchDefault,
          branchEntries.stream().mapToInt(Map.Entry::getKey).toArray(),
          branchEntries.stream().map(x -> x.getValue().label).toArray(LabelNode[]::new)
        ));

        for (var branch : branchEntries) {
          list.add(branch.getValue().label());
          LabelNode paramLabel = null;
          var remaining = branch.getValue().params.size();
          for (var param : branch.getValue().params) {
            remaining--;
            if (paramLabel != null) list.add(paramLabel);
            // [i, args[i], args[i].name]
            list.add(new InsnNode(Opcodes.DUP));
            // [i, args[i], args[i].name, args[i].name]
            list.add(new LdcInsnNode(param.name));
            // [i, args[i], args[i].name, args[i].name, param.name]
            list.add(new MethodInsnNode(
              Opcodes.INVOKEVIRTUAL,
              "java/lang/String",
              "equals",
              "(Ljava/lang/Object;)Z"
            ));
            // [i, args[i], args[i].name, isTheParam]
            paramLabel = remaining == 0 ? switchDefault : new LabelNode();
            list.add(new JumpInsnNode(Opcodes.IFEQ, paramLabel));
            // [i, args[i], args[i].name]
            list.add(new VarInsnNode(Opcodes.ILOAD, helperIntIndex + param.getHelper()));
            // [i, args[i], args[i].name, helper]
            list.add(new LdcInsnNode(param.getHelperBit()));
            // [i, args[i], args[i].name, helper, helperBit]
            list.add(new InsnNode(Opcodes.DUP2));
            // [i, args[i], args[i].name, helper, helperBit, helper, helperBit]
            list.add(new InsnNode(Opcodes.IAND));
            // [i, args[i], args[i].name, helper, helperBit, theBit]
            list.add(new JumpInsnNode(Opcodes.IFNE, alreadyFilledError));
            // [i, args[i], args[i].name, helper, helperBit]
            list.add(new InsnNode(Opcodes.IOR));
            // [i, args[i], args[i].name, modifiedHelper]
            list.add(new VarInsnNode(Opcodes.ISTORE, helperIntIndex + param.getHelper()));
            // [i, args[i], args[i].name]
            list.add(new InsnNode(Opcodes.POP));
            // [i, args[i]]
            list.add(new MethodInsnNode(
              Opcodes.INVOKEVIRTUAL,
              argClass,
              "value",
              "()Ljava/lang/Object;"
            ));
            // [i, args[i].value]
            TypeHelper.convert(list::add, objectType, param.type);
            // [i, value]
            list.add(new VarInsnNode(param.type.getOpcode(Opcodes.ISTORE), param.stack));
            // [i]
            list.add(new JumpInsnNode(Opcodes.GOTO, switchEnd));
          }
        }

        // default ->
        list.add(switchDefault);
        // [i, args[i], args[i].name]
        list.add(new VarInsnNode(Opcodes.ASTORE, tempIndex));
        // [i, args[i]]
        // throw new IllegalArgumentException(...)
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalArgumentException"));
        list.add(new InsnNode(Opcodes.DUP));
        TypeHelper.buildString(
          list::add,
          l -> "unexpected argument name: ",
          l -> {
            l.accept(new VarInsnNode(Opcodes.ALOAD, tempIndex));
            return null;
          }
        );
        list.add(new MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          "java/lang/IllegalArgumentException",
          "<init>",
          "(Ljava/lang/String;)V"
        ));
        list.add(new InsnNode(Opcodes.ATHROW));

        list.add(switchEnd);
        // [i]

        // i++
        list.add(new InsnNode(Opcodes.ICONST_1));
        // [i, 1]
        list.add(new InsnNode(Opcodes.IADD));
        // [i++]
        list.add(new JumpInsnNode(Opcodes.GOTO, loopLabel));

        list.add(entryNullLabel);
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalArgumentException"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new LdcInsnNode("arg items cannot be null"));
        list.add(new MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          "java/lang/IllegalArgumentException",
          "<init>",
          "(Ljava/lang/String;)V"
        ));
        list.add(new InsnNode(Opcodes.ATHROW));

        var notAllFilledError = new LabelNode();
        list.add(notAllFilledError);
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalArgumentException"));
        list.add(new InsnNode(Opcodes.DUP));
        TypeHelper.buildString(
          list::add,
          (l, r) -> {
            r.accept("missing arguments:");
            for (var param : params) {
              var skip = new LabelNode();
              list.add(new VarInsnNode(Opcodes.ILOAD, helperIntIndex + param.getHelper()));
              // [helper]
              list.add(new LdcInsnNode(param.getHelperBit()));
              // [helper, helperBit]
              list.add(new InsnNode(Opcodes.IAND));
              // [theBit]
              list.add(new JumpInsnNode(Opcodes.IFNE, skip));
              // []
              r.accept(" " + param.name);
              // []
              list.add(skip);
              // []
            }
          }
        );
        list.add(new MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          "java/lang/IllegalArgumentException",
          "<init>",
          "(Ljava/lang/String;)V"
        ));
        list.add(new InsnNode(Opcodes.ATHROW));

        for (var param : params) {
          var def = param.defaultValue;
          if (def == null) continue;
          list.add(param.missingDependenciesError);
          list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalArgumentException"));
          list.add(new InsnNode(Opcodes.DUP));
          TypeHelper.buildString(
            list::add,
            (l, r) -> {
              r.accept(param.name + " is missing dependencies:");
              for (var dep : def.dependencies()) {
                var param2 = nameToParam.get(dep.name);
                var skip2 = new LabelNode();
                list.add(new VarInsnNode(Opcodes.ILOAD, helperIntIndex + param2.getHelper()));
                // [helper]
                list.add(new LdcInsnNode(param2.getHelperBit()));
                // [helper, helperBit]
                list.add(new InsnNode(Opcodes.IAND));
                // [theBit]
                list.add(new JumpInsnNode(Opcodes.IFNE, skip2));
                // []
                r.accept(" " + dep.name);
                // []
                list.add(skip2);
                // []
              }
            }
          );
          list.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/lang/IllegalArgumentException",
            "<init>",
            "(Ljava/lang/String;)V"
          ));
          list.add(new InsnNode(Opcodes.ATHROW));
        }

        list.add(breakLabel);
        // [i]
        list.add(new InsnNode(Opcodes.POP));
        // []

        for (var param : params) {
          var def = param.defaultValue;
          if (def == null) continue;

          var skip = new LabelNode();
          list.add(new VarInsnNode(Opcodes.ILOAD, helperIntIndex + param.getHelper()));
          // [helper]
          list.add(new LdcInsnNode(param.getHelperBit()));
          // [helper, helperBit]
          list.add(new InsnNode(Opcodes.IAND));
          // [theBit]
          list.add(new JumpInsnNode(Opcodes.IFNE, skip));
          // []

          var bits = BigInteger.ZERO;
          for (var dep : def.dependencies()) {
            bits = bits.or(nameToParam.get(dep.name).bit);
          }

          for (var i = 0; i < helperCount; i++) {
            var bitsToCompare = bits.intValue();
            bits = bits.shiftRight(32);
            if (bitsToCompare == 0) continue;
            list.add(new VarInsnNode(Opcodes.ILOAD, helperIntIndex + i));
            // [helper]
            list.add(new LdcInsnNode(bitsToCompare));
            // [helper, bits]
            list.add(new InsnNode(Opcodes.IAND));
            // [theBits]
            list.add(new JumpInsnNode(Opcodes.IFEQ, param.missingDependenciesError));
            // []
          }

          list.add(new VarInsnNode(Opcodes.ILOAD, helperIntIndex + param.getHelper()));
          // [helper]
          list.add(new LdcInsnNode(param.getHelperBit()));
          // [helper, helperBit]
          list.add(new InsnNode(Opcodes.IOR));
          // [modifiedHelper]
          list.add(new VarInsnNode(Opcodes.ISTORE, helperIntIndex + param.getHelper()));
          // []
          def.compute(list::add, nameToParam);
          // [value]
          list.add(new VarInsnNode(param.type.getOpcode(Opcodes.ISTORE), param.stack));
          // []
          list.add(skip);
          // []
        }

        for (var i = 0; i < helperCount; i++) {
          list.add(new VarInsnNode(Opcodes.ILOAD, helperIntIndex + i));
          // [helper]
          var rem = params.size() & 0x1F;
          list.add(new LdcInsnNode(
            i == helperCount - 1
            ? rem == 0 ? -1 : ~(-1 << rem)
            : -1
          ));
          // [helper, allFilled]
          list.add(new JumpInsnNode(Opcodes.IF_ICMPNE, notAllFilledError));
          // []
        }

        // delegation
        if (!isStatic) {
          list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        for (var param : params) {
          list.add(new VarInsnNode(param.type.getOpcode(Opcodes.ILOAD), param.stack));
        }
        list.add(new MethodInsnNode(
          invokeMethodOpcode(backingMethod),
          cn.name,
          backingMethod.name,
          backingMethod.desc
        ));
        list.add(new InsnNode(desc.getReturnType().getOpcode(Opcodes.IRETURN)));
      }
    }

    return modified;
  }

  private DefaultValue getDefaultValue(
    ThrowHelper th,
    ClassNode cn,
    Type type,
    String backing,
    String name,
    String annotation,
    boolean isStatic
  ) {
    if (annotation != null && !annotation.isEmpty()) {
      if ("null".equals(annotation)) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
          return (a, m) -> a.accept(new InsnNode(Opcodes.ACONST_NULL));
        } else {
          throw th.raise("null is allowed only on reference types");
        }
      }

      return switch (type.getDescriptor()) {
        case "Z", "Ljava/lang/Boolean;" -> switch (annotation) {
          case "false" -> (a, m) -> {
            a.accept(new InsnNode(Opcodes.ICONST_0));
            TypeHelper.convert(a, Type.BOOLEAN_TYPE, type);
          };
          case "true" -> (a, m) -> {
            a.accept(new InsnNode(Opcodes.ICONST_1));
            TypeHelper.convert(a, Type.BOOLEAN_TYPE, type);
          };
          default -> {
            throw th.raise("only false and true are allowed on boolean");
          }
        };

        case "B", "Ljava/lang/Byte;" -> parseInteger(
          th,
          annotation,
          BigInteger.valueOf(-128L),
          BigInteger.valueOf(255L),
          BigInteger::intValue,
          Type.BYTE_TYPE,
          type
        );

        case "S", "Ljava/lang/Short;" -> parseInteger(
          th,
          annotation,
          BigInteger.valueOf(-32768L),
          BigInteger.valueOf(65535L),
          BigInteger::intValue,
          Type.SHORT_TYPE,
          type
        );

        case "I", "Ljava/lang/Integer;" -> parseInteger(
          th,
          annotation,
          BigInteger.valueOf(Integer.MIN_VALUE),
          BigInteger.valueOf(Integer.MAX_VALUE).multiply(BigInteger.TWO).add(BigInteger.ONE),
          BigInteger::intValue,
          Type.INT_TYPE,
          type
        );

        case "J", "Ljava/lang/Long;" -> parseInteger(
          th,
          annotation,
          BigInteger.valueOf(Long.MIN_VALUE),
          BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO).add(BigInteger.ONE),
          BigInteger::longValue,
          Type.LONG_TYPE,
          type
        );

        case "F", "Ljava/lang/Float;" -> (a, m) -> {
          a.accept(new LdcInsnNode(parseFloat(annotation)));
          TypeHelper.convert(a, Type.FLOAT_TYPE, type);
        };

        case "D", "Ljava/lang/Double;" -> (a, m) -> {
          a.accept(new LdcInsnNode(parseDouble(annotation)));
          TypeHelper.convert(a, Type.DOUBLE_TYPE, type);
        };

        case "C", "Ljava/lang/Character;" -> {
          yield annotation.matches("'.'")
                ?
                (a, m) -> {
                  a.accept(new LdcInsnNode((int) annotation.charAt(1)));
                  TypeHelper.convert(a, Type.CHAR_TYPE, type);
                }
                :
                parseInteger(
                  th,
                  annotation,
                  BigInteger.valueOf(0L),
                  BigInteger.valueOf(65535L),
                  BigInteger::intValue,
                  Type.CHAR_TYPE,
                  type
                );
        }

        case "Ljava/lang/String;" ->
          (a, m) -> a.accept(new LdcInsnNode(annotation));

        default -> {
          throw th.raise("only primitive type, boxed primitive type, String, and null default values can be set directly in @Default");
        }
      };
    }

    var memberName = backing + "$" + name;

    for (var mn : cn.methods) {
      if (memberName.equals(AsmHelper.fromAnnotation(
        mn.invisibleAnnotations,
        "Lyqloss/E$Name;",
        () -> mn.name
      ))) {
        var desc = Type.getMethodType(mn.desc);
        var mnStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        if (!mnStatic && isStatic) {
          throw th.raise("default values for a static method cannot be non-static");
        }
        if (!type.equals(desc.getReturnType())) {
          throw th.raise("default value type does not match parameter type");
        }
        var dependencies =
          IntStream.range(0, desc.getArgumentCount())
            .mapToObj(i -> new TypeAndName(
              desc.getArgumentTypes()[i],
              AsmHelper.fromAnnotation(
                AsmHelper.safeGet(mn.invisibleParameterAnnotations, i),
                "Lyqloss/E$Name;",
                () -> getParamName(th, mn, i)
              )
            ))
            .toList();
        return new DefaultValue() {
          @Override
          public List<TypeAndName> dependencies() {
            return dependencies;
          }

          @Override
          public void compute(Consumer<AbstractInsnNode> accept, Map<String, ParameterInfo> map) {
            if (!mnStatic) {
              accept.accept(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            for (var dep : dependencies) {
              var param = map.get(dep.name);
              accept.accept(new VarInsnNode(param.type.getOpcode(Opcodes.ILOAD), param.stack));
            }
            accept.accept(new MethodInsnNode(
              invokeMethodOpcode(mn),
              cn.name,
              mn.name,
              mn.desc
            ));
          }
        };
      }
    }

    for (var fn : cn.fields) {
      if (memberName.equals(AsmHelper.fromAnnotation(
        fn.invisibleAnnotations,
        "Lyqloss/E$Name;",
        () -> fn.name
      ))) {
        var fnStatic = (fn.access & Opcodes.ACC_STATIC) != 0;
        if (!fnStatic && isStatic) {
          throw th.raise("default values for a static method cannot be non-static");
        }
        if (!type.equals(Type.getType(fn.desc))) {
          throw th.raise("default value type does not match parameter type");
        }
        return (a, m) -> {
          if (!fnStatic) {
            a.accept(new VarInsnNode(Opcodes.ALOAD, 0));
          }
          a.accept(new FieldInsnNode(
            fnStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD,
            cn.name,
            fn.name,
            fn.desc
          ));
        };
      }
    }

    throw th.raise("no default value found for %s", name);
  }

  private DefaultValue parseInteger(
    ThrowHelper th,
    String text,
    BigInteger min,
    BigInteger max,
    Function<BigInteger, Object> transformer,
    Type primitiveType,
    Type expectedType
  ) {
    text = text.toLowerCase().replace("_", "");
    var number = text.startsWith("-0x") ? new BigInteger(text.substring(3), 16).negate() :
                 text.startsWith("0x") ? new BigInteger(text.substring(2), 16) :
                 new BigInteger(text);
    if (min != null && number.compareTo(min) < 0) {
      throw th.raise("value cannot be lower than %s", min);
    }
    if (max != null && number.compareTo(max) > 0) {
      throw th.raise("value cannot be greater than %s", max);
    }
    return (a, m) -> {
      a.accept(new LdcInsnNode(transformer.apply(number)));
      TypeHelper.convert(a, primitiveType, expectedType);
    };
  }

  private float parseFloat(String text) {
    return switch (text.toLowerCase()) {
      case "nan" -> Float.NaN;
      case "infinity" -> Float.POSITIVE_INFINITY;
      case "-infinity" -> Float.NEGATIVE_INFINITY;
      default -> Float.parseFloat(text.replace("_", ""));
    };
  }

  private double parseDouble(String text) {
    return switch (text.toLowerCase()) {
      case "nan" -> Double.NaN;
      case "infinity" -> Double.POSITIVE_INFINITY;
      case "-infinity" -> Double.NEGATIVE_INFINITY;
      default -> Double.parseDouble(text.replace("_", ""));
    };
  }

  private String getParamName(ThrowHelper th, MethodNode mn, int index) {
    if (mn.localVariables == null) {
      throw th.raise("local variable info is not present, try adding -g javac argument or use @Name for every parameter");
    }
    var desc = Type.getMethodType(mn.desc);
    var slot = (mn.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
    for (int i = 0; i < index; i++) {
      slot += desc.getArgumentTypes()[i].getSize();
    }
    for (var variable : mn.localVariables) {
      if (variable.index == slot) {
        return variable.name;
      }
    }
    throw th.raise("parameter name at index %d not found", index);
  }

  private int invokeMethodOpcode(MethodNode mn) {
    if ((mn.access & Opcodes.ACC_STATIC) != 0) {
      return Opcodes.INVOKESTATIC;
    }
    if ((mn.access & Opcodes.ACC_PRIVATE) != 0) {
      return Opcodes.INVOKESPECIAL;
    }
    if ("<init>".equals(mn.name)) {
      return Opcodes.INVOKESPECIAL;
    }
    return Opcodes.INVOKEVIRTUAL;
  }
}
