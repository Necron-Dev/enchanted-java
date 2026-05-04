package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import net.yqloss.enchant.plugin.pass.TypeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public enum ArgPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    var argClass = AsmHelper.fromAnnotation(
      cn.invisibleAnnotations,
      "Lyqloss/E$ArgClass;",
      (Type t) -> t.getInternalName(),
      () -> cn.name + "$Arg"
    );

    for (var mn : cn.methods) {
      var th = new ThrowHelper("arg", cn, mn);
      if (AsmHelper.containsStub(mn.instructions, "_arg", "_arg_")) {
        modified = true;
        var desc = Type.getMethodType(mn.desc);
        if (
          (mn.access & Opcodes.ACC_STATIC) == 0
          || desc.getArgumentCount() != 1
          || desc.getReturnType().getSort() != Type.OBJECT
          || !argClass.equals(desc.getReturnType().getInternalName())
        ) {
          throw th.raise(
            "_arg methods must be static, contain exactly one argument, and return %s",
            argClass
          );
        }
        mn.tryCatchBlocks.clear();
        var list = mn.instructions;
        list.clear();
        var type = desc.getArgumentTypes()[0];
        var argName = AsmHelper.fromAnnotation(
          mn.invisibleAnnotations,
          "Lyqloss/E$Name;",
          () -> mn.name.substring(mn.name.lastIndexOf('_') + 1)
        );
        list.add(new TypeInsnNode(Opcodes.NEW, argClass));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new LdcInsnNode(argName));
        list.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), 0));
        TypeHelper.convert(list::add, type, Type.getType(Object.class));
        list.add(new MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          argClass,
          "<init>",
          "(Ljava/lang/String;Ljava/lang/Object;)V"
        ));
        list.add(new InsnNode(Opcodes.ARETURN));
      }
    }

    return modified;
  }
}
