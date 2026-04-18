package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.Pass;
import net.yqloss.enchant.plugin.TypeConverter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public enum ReturnPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (iter.next() instanceof MethodInsnNode min
          && min.getOpcode() == Opcodes.INVOKESTATIC
          && Enchanter.EnchantedJavaClasses.contains(min.owner)
          && "_return".equals(min.name)
        ) {
          modified = true;
          iter.remove();
          var returnType = Type.getReturnType(mn.desc);
          switch (min.desc) {
            case "()Ljava/lang/Object;" ->
              TypeConverter.convert(iter::add, Type.VOID_TYPE, returnType);

            case "(Ljava/lang/Object;)Ljava/lang/Object;" ->
              TypeConverter.convert(iter::add, Type.getType(Object.class), returnType);

            default ->
              throw new UnsupportedOperationException("unknown _return signature " + min.desc);
          }
          iter.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        }
      }
    }
    return modified;
  }
}
