package net.yqloss.enchant.plugin.pass.enchant;

import net.yqloss.enchant.plugin.Enchanter;
import net.yqloss.enchant.plugin.pass.AsmHelper;
import net.yqloss.enchant.plugin.pass.Pass;
import net.yqloss.enchant.plugin.pass.Serializer;
import net.yqloss.enchant.plugin.pass.ThrowHelper;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public enum CompileTimePass implements Pass {
  Instance;

  @FunctionalInterface
  private interface ClassLoaderFn {
    Class<?> load(String name, boolean resolve) throws ClassNotFoundException;
  }

  @Override
  public boolean accept(ClassNode cn, ClassLoader classLoader) {
    var fields = new ArrayDeque<String>();

    var modified = false;
    for (var mn : cn.methods) {
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && min.getOpcode() == Opcodes.INVOKESTATIC
          && Enchanter.EnchantedJavaClasses.contains(min.owner)
          && "_const".equals(min.name)
        ) {
          modified = true;
          iter.remove();
          iter.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "net/yqloss/enchant/plugin/pass/Marker",
            min.name,
            min.desc
          ));
          var field = AsmHelper.findUniqueName(
            cn.fields.stream().map(x -> x.name),
            "$$enchantedJava$compileTime"
          );
          fields.add(field);
          cn.fields.add(new FieldNode(0, field, "Ljava/lang/Object;", null, null));
        }
      }
    }
    if (!modified) return false;

    var markedClass = Enchanter.classNodeToBytes(cn, classLoader);
    var className = cn.name.replace('/', '.');
    var thisClassLoader = getClass().getClassLoader();
    var wrappedLoader = new ClassLoader(classLoader) {
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return
          "net.yqloss.enchant.plugin.pass.Marker".equals(name)
          ? Class.forName(name, resolve, thisClassLoader) :
          className.equals(name)
          ? loadTheClass(name, resolve) :
          firstSuccessful(
            name,
            resolve,
//            (_name, _resolve) -> Class.forName(name, resolve, thisClassLoader),
            super::loadClass
          );
      }

      private Class<?> firstSuccessful(
        String name,
        boolean resolve,
        ClassLoaderFn... loaders
      ) throws ClassNotFoundException {
        for (var loader : loaders) {
          try {
            return loader.load(name, resolve);
          } catch (Exception exception) {
            throw new RuntimeException(exception);
          }
        }
        throw new ClassNotFoundException();
      }

      private Class<?> loadTheClass(String name, boolean resolve) {
        synchronized (getClassLoadingLock(name)) {
          Class<?> c = findLoadedClass(name);
          if (c == null) {
            c = defineClass(name, markedClass, 0, markedClass.length);
          }
          if (resolve) {
            resolveClass(c);
          }
          return c;
        }
      }
    };

    Class<?> theClass;
    try {
      theClass = Class.forName(className, true, wrappedLoader);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }

    var initializers = new LinkedHashMap<String, Consumer<Consumer<AbstractInsnNode>>>();

    for (var mn : cn.methods) {
      var th = new ThrowHelper("compile-time", cn, mn);
      var iter = mn.instructions.iterator();
      while (iter.hasNext()) {
        if (
          iter.next() instanceof MethodInsnNode min
          && min.getOpcode() == Opcodes.INVOKESTATIC
          && "net/yqloss/enchant/plugin/pass/Marker".equals(min.owner)
          && "_const".equals(min.name)
        ) {
          iter.remove();
          if (
            iter.previous() instanceof InvokeDynamicInsnNode idin
            && idin.bsmArgs[1] instanceof Handle handle
            && handle.getTag() == Opcodes.H_INVOKESTATIC
            && handle.getDesc().matches("\\(\\).*")
          ) {
            iter.remove();
            var field = fields.pop();
            iter.add(new FieldInsnNode(
              Opcodes.GETSTATIC,
              cn.name,
              field,
              "Ljava/lang/Object;"
            ));
            var method =
              Arrays.stream(theClass.getDeclaredMethods())
                .filter(x -> handle.getName().equals(x.getName()))
                .findFirst()
                .orElse(null);
            if (method == null) {
              throw th.raise("lambda method %s not found", handle.getName());
            }
            method.setAccessible(true);
            Object value;
            try {
              value = method.invoke(null);
            } catch (Exception exception) {
              throw th.raise(exception, "failed to compute value");
            }
            initializers.put(field, emit -> Serializer.INSTANCE.serialize(value, emit));
          } else {
            throw th.raise("_const can only be used on a lambda without captures");
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

    return true;
  }
}
