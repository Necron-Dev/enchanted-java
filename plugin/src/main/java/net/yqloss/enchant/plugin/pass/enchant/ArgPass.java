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
    for (var mn : cn.methods) {
      var th = new ThrowHelper("arg", cn, mn);
      if (AsmHelper.containsStub(mn.instructions, "_arg", "_arg_")) {
        modified = true;
        var desc = Type.getMethodType(mn.desc);
        var isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        var useThis = false;
        if (desc.getArgumentCount() == 0 && !isStatic) {
          useThis = true;
        } else if (
                 desc.getArgumentCount() != 1
                 || desc.getReturnType().getSort() != Type.OBJECT
        ) {
          throw th.raise("static _arg methods must take exactly one argument; non-static _arg methods must take zero or one argument");
        }
        var argClass = desc.getReturnType().getInternalName();
        mn.tryCatchBlocks.clear();
        var list = mn.instructions;
        list.clear();
        var type = useThis
                   ? Type.getType("L" + cn.name + ";")
                   : desc.getArgumentTypes()[0];
        var argName = AsmHelper.fromAnnotation(
          mn.invisibleAnnotations,
          "Lyqloss/E$Name;",
          () -> mn.name.substring(mn.name.lastIndexOf('_') + 1).replace("$", "")
        );
        list.add(new TypeInsnNode(Opcodes.NEW, argClass));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new LdcInsnNode(argName));
        list.add(new VarInsnNode(
          type.getOpcode(Opcodes.ILOAD),
          isStatic || useThis ? 0 : 1
        ));
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
