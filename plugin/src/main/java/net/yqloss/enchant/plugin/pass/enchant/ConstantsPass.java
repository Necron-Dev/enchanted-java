package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.UUID;

public enum ConstantsPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn) {
    var modified = false;
    var uuidMap = new HashMap<String, UUID>();
    for (var mn : cn.methods) {
      var th = new ThrowHelper("const", cn, mn);
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (iter.next() instanceof FieldInsnNode fin
            && fin.getOpcode() == Opcodes.GETSTATIC
            && Enchanter.EnchantedJavaClasses.contains(fin.owner)
        ) {
          modified = true;
          iter.remove();
          switch (fin.name) {
            case "_any", "_switch" -> iter.add(new InsnNode(Opcodes.ICONST_0));
            case "_all" -> iter.add(new InsnNode(Opcodes.ICONST_1));
            case "_id" -> {
              var uuidField = AsmHelper.findUniqueName(
                cn.fields.stream().map(it -> it.name),
                "$$enchantedJava$uniqueId"
              );
              iter.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                cn.name,
                uuidField,
                "Ljava/util/UUID;"
              ));
              uuidMap.put(uuidField, UUID.randomUUID());
              cn.fields.add(new FieldNode(0, uuidField, null, null, null));
            }
            default -> throw th.raise("unknown constant %s", fin.name);
          }
        }
      }
    }
    for (var entry : uuidMap.entrySet()) {
      AsmHelper.createInternalField(
        cn,
        entry.getKey(),
        "Ljava/util/UUID;",
        append -> {
          append.accept(new TypeInsnNode(Opcodes.NEW, "java/util/UUID"));
          append.accept(new InsnNode(Opcodes.DUP));
          append.accept(new LdcInsnNode(entry.getValue().getMostSignificantBits()));
          append.accept(new LdcInsnNode(entry.getValue().getLeastSignificantBits()));
          append.accept(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/util/UUID",
            "<init>",
            "(JJ)V",
            false
          ));
        }
      );
    }
    return modified;
  }
}
