package net.yqloss.enchant.plugin.pass;

import org.objectweb.asm.tree.ClassNode;

public interface Pass {
  boolean accept(ClassNode cn, ClassLoader classLoader);
}
