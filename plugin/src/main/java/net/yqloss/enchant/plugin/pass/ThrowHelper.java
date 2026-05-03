package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public record ThrowHelper(
  String module,
  ClassNode cn,
  MethodNode mn
) {
  public UnsupportedOperationException raise(String desc, Object... params) {
    return raise(null, desc, params);
  }

  public UnsupportedOperationException raise(Throwable cause, String desc, Object... params) {
    return new UnsupportedOperationException(
      String.format(
        "[%s] %s\n  in %s%s of %s",
        module, String.format(desc, params), mn.name, mn.desc, cn.name
      ), cause
    );
  }
}
