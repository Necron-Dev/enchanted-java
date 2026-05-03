package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.pass.Pass;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public enum TrimAnnotationPass implements Pass {
  Instance;

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var modified = false;

    modified |= remove(cn.invisibleAnnotations);
    modified |= remove(cn.visibleAnnotations);
    modified |= remove(cn.invisibleTypeAnnotations);
    modified |= remove(cn.visibleTypeAnnotations);

    for (var mn : cn.methods) {
      modified |= remove(mn.invisibleAnnotations);
      modified |= remove(mn.visibleAnnotations);
      modified |= remove(mn.invisibleTypeAnnotations);
      modified |= remove(mn.visibleTypeAnnotations);
      modified |= remove(mn.invisibleLocalVariableAnnotations);
      modified |= remove(mn.visibleLocalVariableAnnotations);

      if (mn.invisibleParameterAnnotations != null) {
        for (var param : mn.invisibleParameterAnnotations) {
          modified |= remove(param);
        }
      }

      if (mn.visibleParameterAnnotations != null) {
        for (var param : mn.visibleParameterAnnotations) {
          modified |= remove(param);
        }
      }
    }

    for (var fn : cn.fields) {
      modified |= remove(fn.invisibleAnnotations);
      modified |= remove(fn.visibleAnnotations);
      modified |= remove(fn.invisibleTypeAnnotations);
      modified |= remove(fn.visibleTypeAnnotations);
    }

    return modified;
  }

  private boolean remove(List<? extends AnnotationNode> annotations) {
    if (annotations == null) return false;
    return annotations.removeIf(a -> a.desc.startsWith("Lyqloss/E$"));
  }
}
