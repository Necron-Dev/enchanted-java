package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public enum ReceiverPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;
    var objectType = Type.getType(Object.class);
    for (var mn : cn.methods) {
      var th = new ThrowHelper("receiver", cn, mn);
      var hasReceiver = false;
      var receiverVar = mn.maxLocals;
      {
        var iter = mn.instructions.iterator();
        while (iter.hasNext()) {
          if (
            iter.next() instanceof MethodInsnNode min
            && AsmHelper.isCallHook(min, "_receiver")
          ) {
            modified = true;
            hasReceiver = true;
            iter.set(new VarInsnNode(Opcodes.ASTORE, receiverVar));
          }
        }
      }
      if (!hasReceiver) continue;
      mn.maxLocals += 1;
      Analyzed.analyzed(
        th, cn, mn,
        analyzed -> {
          var modifiedMethod = false;
          for (var i = analyzed.size() - 1; i >= 0; i--) {
            var item = analyzed.get(i);
            if (
              item.insn().getOpcode() == Opcodes.POP
              || item.insn().getOpcode() == Opcodes.POP2
            ) {
              var stackSize = AsmHelper.getStackSize(item.frame());
              if (stackSize == -1) continue;
              modifiedMethod = true;
              var stackTop = item.frame().getStack(stackSize - 1).getType();
              var list = new InsnList();
              var skip = new LabelNode();
              var done = new LabelNode();
              list.add(new VarInsnNode(Opcodes.ALOAD, receiverVar));
              list.add(new JumpInsnNode(Opcodes.IFNULL, skip));
              TypeHelper.convert(list::add, stackTop, objectType);
              list.add(new VarInsnNode(Opcodes.ALOAD, receiverVar));
              list.add(new InsnNode(Opcodes.SWAP));
              list.add(new LdcInsnNode(i));
              list.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/function/ObjIntConsumer",
                "accept",
                "(Ljava/lang/Object;I)V"
              ));
              list.add(new JumpInsnNode(Opcodes.GOTO, done));
              list.add(skip);
              list.add(new InsnNode(item.insn().getOpcode()));
              list.add(done);
              Analyzed.removeNext(analyzed, i, 1);
              Analyzed.insert(analyzed, i, list);
            }
          }
          return modifiedMethod;
        }
      );
      mn.instructions.insert(new VarInsnNode(Opcodes.ASTORE, receiverVar));
      mn.instructions.insert(new InsnNode(Opcodes.ACONST_NULL));
    }
    return modified;
  }
}
