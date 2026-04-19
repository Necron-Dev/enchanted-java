package net.yqloss.enchant.plugin;

import org.objectweb.asm.tree.ClassNode;

public interface Pass {
  boolean accept(ClassNode cn);
}
