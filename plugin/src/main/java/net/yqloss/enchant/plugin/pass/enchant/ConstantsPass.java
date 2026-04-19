package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;

public enum ConstantsPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn) {
    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (iter.next() instanceof FieldInsnNode fin
          && fin.getOpcode() == Opcodes.GETSTATIC
          && Enchanter.EnchantedJavaClasses.contains(fin.owner)
        ) {
          modified = true;
          iter.remove();
          switch (fin.name) {
            case "_any" -> iter.add(new InsnNode(Opcodes.ICONST_0));
            case "_all" -> iter.add(new InsnNode(Opcodes.ICONST_1));
            default ->
              throw new UnsupportedOperationException("unknown constant " + fin.name);
          }
        }
      }
    }
    return modified;
  }
}
