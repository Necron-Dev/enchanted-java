package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import net.yqloss.enchant.plugin.pass.TypeConverter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum ReturnPass implements Pass {
  Instance;

  private final Type objectType = Type.getType(Object.class);

  private final Type throwableType = Type.getType(Throwable.class);

  private TryCatchBlockNode locateFinally(List<TryCatchBlockNode> finallyBlocks, Set<Label> visitedLabels) {
    return finallyBlocks.stream().filter(
      it -> it.type == null
            && visitedLabels.contains(it.start.getLabel())
            && !visitedLabels.contains(it.end.getLabel())
    ).findFirst().orElse(null);
  }

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    var needToCacheThrowable = false;
    var cachedThrowable = AsmHelper.findUniqueName(
      cn.fields.stream().map(it -> it.name),
      "$$enchantedJava$cachedThrowable"
    );

    for (var mn : cn.methods) {
      var th = new ThrowHelper("return", cn, mn);
      var resultObject = mn.maxLocals;
      var throwableObject = mn.maxLocals + 1;
      var returnLabel = new LabelNode();
      var needToHookFinally = false;
      var visitedLabels = new HashSet<Label>();
      var returnType = Type.getReturnType(mn.desc);
      var iter = mn.instructions.iterator();

      var finallyBlocks = new ArrayList<TryCatchBlockNode>();
      for (var tryCatch : mn.tryCatchBlocks) {
        if (tryCatch.type != null) continue;
        finallyBlocks.add(tryCatch);
      }

      while (iter.hasNext()) {
        var insn = iter.next();

        if (insn instanceof LabelNode label) {
          visitedLabels.add(label.getLabel());
          continue;
        }

        if (
          insn instanceof MethodInsnNode min
          && min.getOpcode() == Opcodes.INVOKESTATIC
          && Enchanter.EnchantedJavaClasses.contains(min.owner)
          && switch (min.name) {
            case "_return", "_return_", "$return" -> true;
            default -> false;
          }
        ) {
          var nullable = "$return".equals(min.name);
          var label = new LabelNode();
          var params = TypeConverter.extractParameters(min.desc);
          modified = true;
          iter.remove();

          if (nullable) {
            iter.add(new InsnNode(Opcodes.DUP));
            iter.add(new JumpInsnNode(Opcodes.IFNULL, label));
          }

          var finallyBlock = locateFinally(finallyBlocks, visitedLabels);
          if (finallyBlock == null) {
            switch (params) {
              case "" ->
                TypeConverter.convert(th, iter::add, Type.VOID_TYPE, returnType);

              case "Ljava/lang/Object;" ->
                TypeConverter.convert(th, iter::add, objectType, returnType);

              default ->
                throw th.raise("unknown return signature %s%s", min.name, min.desc);
            }

            iter.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
          } else {
            if (returnType.getSort() == Type.VOID) {
              switch (params) {
                case "" -> {
                }

                case "Ljava/lang/Object;" ->
                  iter.add(new InsnNode(Opcodes.POP));

                default ->
                  throw th.raise("unknown return signature %s%s", min.name, min.desc);
              }
            } else {
              switch (params) {
                case "" -> // throws because this convert never succeeds
                  TypeConverter.convert(th, iter::add, Type.VOID_TYPE, returnType);

                case "Ljava/lang/Object;" ->
                  iter.add(new VarInsnNode(Opcodes.ASTORE, resultObject));

                default ->
                  throw th.raise("unknown return signature %s%s", min.name, min.desc);
              }
            }

            var labelBegin = new LabelNode();
            var labelEnd = new LabelNode();

            iter.add(new FieldInsnNode(
              Opcodes.GETSTATIC,
              cn.name,
              cachedThrowable,
              throwableType.getDescriptor()
            ));
            iter.add(new InsnNode(Opcodes.DUP));
            iter.add(new VarInsnNode(Opcodes.ASTORE, throwableObject));
            iter.add(labelBegin);
            iter.add(new InsnNode(Opcodes.ATHROW));
            iter.add(labelEnd);
            mn.tryCatchBlocks.add(0, new TryCatchBlockNode(labelBegin, labelEnd, finallyBlock.handler, null));
            needToHookFinally = true;
          }

          if (nullable) {
            iter.add(label);
          }
        }
      }

      if (needToHookFinally) {
        needToCacheThrowable = true;

        var iter2 = mn.instructions.iterator();
        var visitedLabels2 = new HashSet<Label>();

        while (iter2.hasNext()) {
          var insn = iter2.next();

          if (insn instanceof LabelNode label) {
            visitedLabels2.add(label.getLabel());
            continue;
          }

          if (insn instanceof VarInsnNode
              && insn.getOpcode() == Opcodes.ALOAD
              && AsmHelper.nextExecutable(insn) instanceof InsnNode nextInsn
              && nextInsn.getOpcode() == Opcodes.ATHROW
          ) {
            var finallyBlock = locateFinally(finallyBlocks, visitedLabels2);
            var jumpTo = finallyBlock == null ? returnLabel : finallyBlock.handler;
            var labelBegin = new LabelNode();
            var labelEnd = new LabelNode();

            iter2.add(new InsnNode(Opcodes.DUP));
            iter2.add(new VarInsnNode(Opcodes.ALOAD, throwableObject));
            iter2.add(new JumpInsnNode(Opcodes.IF_ACMPNE, labelEnd));
            iter2.add(labelBegin);
            iter2.add(new InsnNode(Opcodes.ATHROW));
            iter2.add(labelEnd);
            mn.tryCatchBlocks.add(0, new TryCatchBlockNode(labelBegin, labelEnd, jumpTo, null));
          }
        }

        mn.maxLocals += 2;
        var list = new InsnList();
        list.add(new InsnNode(Opcodes.ACONST_NULL));
        list.add(new VarInsnNode(Opcodes.ASTORE, resultObject));
        list.add(new InsnNode(Opcodes.ACONST_NULL));
        list.add(new VarInsnNode(Opcodes.ASTORE, throwableObject));
        mn.instructions.insert(list);
        list = new InsnList();
        list.add(returnLabel);
        list.add(new InsnNode(Opcodes.POP));
        if (returnType.getSort() == Type.VOID) {
          list.add(new InsnNode(Opcodes.RETURN));
        } else {
          list.add(new VarInsnNode(Opcodes.ALOAD, resultObject));
          TypeConverter.convert(th, list::add, objectType, returnType);
          list.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        }
        mn.instructions.add(list);
      }
    }

    if (needToCacheThrowable) {
      AsmHelper.createInternalField(
        cn,
        cachedThrowable,
        throwableType.getDescriptor(),
        append -> {
          append.accept(new TypeInsnNode(Opcodes.NEW, throwableType.getInternalName()));
          append.accept(new InsnNode(Opcodes.DUP));
          append.accept(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            throwableType.getInternalName(),
            "<init>",
            "()V",
            false
          ));
        }
      );
    }

    return modified;
  }
}
