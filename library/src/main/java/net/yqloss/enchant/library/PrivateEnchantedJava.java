package net.yqloss.enchant.library;

class PrivateEnchantedJava {
  public static boolean _any = false;

  public static boolean _all = true;

  public static void _pass() {
  }

  public static <T> T _throw(Throwable throwable) {
    throw new UnenchantedException();
  }

  public static RuntimeException $throw(Throwable throwable) {
    throw new UnenchantedException();
  }

  public static <T> T _never() {
    throw new UnenchantedException();
  }

  public static RuntimeException $never() {
    throw new UnenchantedException();
  }

  public static <T> T _return() {
    throw new UnenchantedException();
  }

  public static RuntimeException $return() {
    throw new UnenchantedException();
  }

  public static <T> T _return(Object value) {
    throw new UnenchantedException();
  }

  public static RuntimeException $return(Object value) {
    throw new UnenchantedException();
  }

  @SafeVarargs
  public static <T> T _elvis(T... values) {
    return values[0];
  }
}
