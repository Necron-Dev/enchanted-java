package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public record ThrowHelper(
  String module,
  ClassNode cn,
  MethodNode mn
) {
  public UnsupportedOperationException raise(String desc, Object... params) {
    return new UnsupportedOperationException(String.format(
      "[%s] <%s.%s%s> %s",
      module, cn.name, mn.name, mn.desc, String.format(desc, params)
    ));
  }

  public UnsupportedOperationException raise(Throwable cause, String desc, Object... params) {
    return new UnsupportedOperationException(
      String.format(
        "[%s] (%s.%s%s) %s",
        module, cn.name, mn.name, mn.desc, String.format(desc, params)
      ), cause
    );
  }
}
