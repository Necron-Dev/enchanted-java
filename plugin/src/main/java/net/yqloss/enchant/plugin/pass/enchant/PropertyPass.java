package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.Serializer;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PropertyPass implements Pass {
  private final Map<String, ?> properties;

  public PropertyPass(Map<String, ?> properties) {
    this.properties = properties;
  }

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var initializers = new LinkedHashMap<String, Consumer<Consumer<AbstractInsnNode>>>();

    var modified = false;
    for (var mn : cn.methods) {
      var th = new ThrowHelper("property", cn, mn);
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && AsmHelper.isCallHook(min, "_property")
        ) {
          modified = true;
          iter.remove();
          if (
            iter.previous() instanceof LdcInsnNode lin
            && lin.cst instanceof String name
          ) {
            if (!properties.containsKey(name)) {
              throw th.raise("property not found: %s", name);
            }
            iter.remove();
            var field = AsmHelper.findUniqueName(
              cn.fields.stream().map(x -> x.name),
              "$$enchantedJava$property"
            );
            cn.fields.add(new FieldNode(0, field, null, null, null));
            iter.add(new FieldInsnNode(
              Opcodes.GETSTATIC,
              cn.name,
              field,
              "Ljava/lang/Object;"
            ));
            var value = properties.get(name);
            initializers.put(field, emit -> Serializer.INSTANCE.serialize(value, emit));
          } else {
            throw th.raise("_property can only be used on string constants");
          }
        }
      }
    }

    for (var init : initializers.entrySet()) {
      AsmHelper.createInternalField(
        cn,
        init.getKey(),
        "Ljava/lang/Object;",
        init.getValue()
      );
    }

    return modified;
  }
}
