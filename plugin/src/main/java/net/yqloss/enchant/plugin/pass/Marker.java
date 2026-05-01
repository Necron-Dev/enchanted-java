package net.yqloss.enchant.plugin.pass;

import java.util.function.Supplier;

public class Marker {
  public static <T> T _const(Supplier<? extends T> expr) {
    return expr.get();
  }
}
