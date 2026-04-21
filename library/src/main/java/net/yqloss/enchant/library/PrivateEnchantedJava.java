package net.yqloss.enchant.library;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The base implementation class for Enchanted Java.
 * <p>
 * This class serves as a collection of <b>compile-time tokens</b> for bytecode
 * transformation. References to these fields and calls to these methods are
 * intercepted and replaced by a transformer (the Gradle plugin).
 * <p>
 * At runtime, the resulting bytecode will have <b>no dependency</b> on this
 * class.
 *
 * @see E
 * @see Ench
 * @see EnchantedJava
 */
@SuppressWarnings("unused")
class PrivateEnchantedJava {
  private static <T> T unenchanted() {
    throw new UnenchantedException();
  }

  /**
   * Provides a leading "false" anchor for chained OR ({@code ||}) logical
   * expressions to simplify reordering or commenting lines. Effectively
   * equivalent to {@code false}, this non-final field prevents compilers from
   * issuing "constant condition" or "unreachable code" warnings. At runtime,
   * the Gradle plugin replaces this reference with the constant {@code false}.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * if (_any
   *   || x < MIN_X
   *   || x > MAX_X
   *   || y < MIN_Y
   *   || y > MAX_Y
   * ) return;
   * }</pre>
   *
   * @see #_all
   * @see #_switch
   */
  public static boolean _any = unenchanted();

  /**
   * Provides a leading "true" anchor for chained AND ({@code &&}) logical
   * expressions. Effectively equivalent to {@code true}, this non-final field
   * prevents compilers from issuing "constant condition" warnings while
   * allowing for a more uniform code style. At runtime, the Gradle plugin
   * replaces this reference with the constant {@code true}.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * if (_all
   *   && other instanceof Pair pair
   *   && Objects.equals(first, other.first)
   *   && Objects.equals(second, other.second)
   * ) return true;
   * }</pre>
   *
   * @see #_any
   * @see #_switch
   */
  public static boolean _all = unenchanted();

  /**
   * Provides a leading "false" anchor for chained ternary ({@code ?:})
   * expressions to create a readable "pseudo-switch" structure. Effectively
   * equivalent to {@code false}, this non-final field suppresses IDE "always
   * false" inspections. At runtime, the Gradle plugin replaces this reference
   * with the constant {@code false}.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * var exceptionToThrow = _switch ||
   *   i < 0 && s == null ? new IllegalArgumentException() :
   *   i >= 0 && s != null ? new IllegalArgumentException() :
   *   i >= size() ? new IndexOutOfBoundsException() :
   *   null;
   * if (exceptionToThrow != null) throw exceptionToThrow;
   * }</pre>
   *
   * @see #_any
   * @see #_all
   */
  public static boolean _switch = unenchanted();

  /**
   * Performs no operation (No-op), serving as an explicit placeholder for empty
   * loops, branches, or method bodies to signal intentional inactivity. This
   * method is equivalent to an empty block {@code {}} and is used to suppress
   * "empty statement" warnings. The Gradle plugin removes this call entirely
   * from the resulting bytecode.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Explicit busy-wait loop
   * while (isLocked) _pass();
   *
   * // Explicitly doing nothing in a specific branch
   * if (isIgnored) _pass();
   * else handle(event);
   * }</pre>
   */
  public static void _pass() {
    unenchanted();
  }

  /**
   * Throws a {@link Throwable} within an expression context, such as a ternary
   * operator branch. This method is a placeholder for a Gradle plugin
   * transformation; at runtime, the call is replaced by a direct {@code athrow}
   * instruction that terminates the control flow immediately. This enables
   * "throw as expression" functionality, allowing exceptions to be raised in
   * positions where Java statements are normally prohibited.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Using throw as an expression in a ternary operator
   * var result = (input != null) ? input : _throw(new IllegalArgumentException());
   * }</pre>
   *
   * @param throwable the exception to be thrown.
   * @param <T>       the inferred common type to allow use in any expression.
   * @return this method never returns normally.
   */
  public static <T> T _throw(Throwable throwable) {
    throw new UnenchantedException();
  }

  /**
   * Throws a {@link Throwable}, designed to be used as {@code throw $throw(e)}.
   * While the {@code throw} keyword satisfies Java's reachability analysis, the
   * Gradle plugin transforms the {@code $throw()} call into a direct
   * {@code athrow} instruction. Execution is terminated by the method call
   * itself, meaning the outer {@code throw} is never executed. This approach
   * allows any exception to be thrown without adding it to the method's
   * {@code throws} signature.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Sneaky throw: throwing a checked exception without a throws clause
   * public void runTask() {
   *     throw $throw(new Exception("Checked exception"));
   * }
   * }</pre>
   *
   * @param throwable the exception to be thrown.
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException $throw(Throwable throwable) {
    throw new UnenchantedException();
  }

  /**
   * Marks a code path as unreachable within an expression context. This method
   * is a placeholder for a Gradle plugin transformation; at runtime, the call
   * is replaced by a direct instruction that throws an
   * {@link IllegalStateException}. Since the transformation interrupts the
   * control flow immediately, this method never returns a value.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Use in a ternary expression where the null branch is logically impossible
   * var result = (status != null) ? status : _never();
   * }</pre>
   *
   * @param <T> the inferred common type to allow use in any expression.
   * @return this method never returns normally.
   */
  public static <T> T _never() {
    throw new UnenchantedException();
  }

  /**
   * Marks a code path as unreachable, designed to be used as
   * {@code throw $never()}. While the {@code throw} keyword satisfies Java's
   * reachability analysis, the Gradle plugin transforms the {@code $never()}
   * call into a direct {@link IllegalStateException} throw. Effectively,
   * execution is terminated by the method call itself, and the outer
   * {@code throw} is never executed. This approach allows for interrupting
   * control flow without adding to the method's {@code throws} signature.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Use as a terminal placeholder for paths the compiler cannot prove are unreachable
   * for (var item : items) {
   *     if (process(item)) return;
   * }
   * throw $never();
   * }</pre>
   *
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException $never() {
    throw new UnenchantedException();
  }

  /**
   * Performs a return from the current method within an expression context.
   * This method is a placeholder for a Gradle plugin transformation that
   * replaces the call with an appropriate return instruction. It provides full
   * support for {@code finally} blocks and {@code synchronized} monitors,
   * ensuring resources are correctly released before the method exits.
   * <p>
   * <b>Usage Constraint:</b> This variant is strictly intended for methods
   * with a {@code void} return type.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Using return as an expression to exit a void method early
   * (input == null) ? _return() : process(input);
   * }</pre>
   *
   * @param <T> the inferred common type to allow use in any expression.
   * @return this method never returns normally.
   */
  public static <T> T _return() {
    throw new UnenchantedException();
  }

  /**
   * Performs a return from the current method, designed to be used as
   * {@code throw $return()}. While the {@code throw} keyword satisfies Java's
   * reachability analysis, the Gradle plugin replaces the call with a direct
   * return instruction. Execution is terminated by the method call itself,
   * ensuring full support for {@code finally} blocks and {@code synchronized}
   * monitors before exiting.
   * <p>
   * <b>Usage Constraint:</b> This variant is strictly intended for methods
   * with a {@code void} return type.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * public void doWork() {
   *   if (isDone()) throw $return();
   *   // ... rest of the logic
   * }
   * }</pre>
   *
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException $return() {
    throw new UnenchantedException();
  }

  /**
   * Performs a return with a value from the current method within an expression
   * context. The Gradle plugin replaces this call with a return instruction and
   * ensures that {@code finally} blocks and {@code synchronized} monitors are
   * correctly processed.
   * <p>
   * <b>Transformation Logic:</b>
   * <ul>
   *   <li>If the current method returns {@code void}, the {@code value} is
   *       ignored and the method exits.</li>
   *   <li>If the method returns a primitive type, the {@code value} must
   *       exactly match the corresponding wrapper type (e.g., {@link Integer}
   *       for {@code int}). No implicit widening or narrowing conversions
   *       are performed.</li>
   *   <li>If the method returns an object type, a {@code checkcast} is
   *       applied to the {@code value} before returning.</li>
   * </ul>
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Get a valid value or exit early with a default value as an expression
   * var value = isValid(data) ? data.getValue() : _return("Default");
   * }</pre>
   *
   * @param value the value to be returned by the parent method.
   * @param <T>   the inferred common type to allow use in any expression.
   * @return this method never returns normally.
   */
  public static <T> T _return(Object value) {
    throw new UnenchantedException();
  }

  /**
   * Performs a return with a value from the current method, designed to be used
   * as {@code throw $return(value)}. The Gradle plugin transforms this call
   * into a direct return instruction, handling {@code finally} and
   * {@code synchronized} blocks correctly. The outer {@code throw} is never
   * executed, but serves to satisfy the compiler's return requirements.
   * <p>
   * <b>Transformation Logic:</b>
   * <ul>
   *   <li>If the current method returns {@code void}, the {@code value} is
   *       ignored and the method exits.</li>
   *   <li>If the method returns a primitive type, the {@code value} (passed as
   *       a wrapper) must exactly match the expected wrapper type. No implicit
   *       conversions (like {@code long} to {@code int}) are performed.</li>
   *   <li>If the method returns an object type, a {@code checkcast} is applied
   *       to the {@code value} before the return instruction.</li>
   * </ul>
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * public int getValue(boolean flag) {
   *     if (flag) throw $return(1); // Exact match for Integer wrapper
   *     return 0;
   * }
   * }</pre>
   *
   * @param value the value to be returned by the parent method.
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException $return(Object value) {
    throw new UnenchantedException();
  }

  /**
   * Mimics the null-coalescing (Elvis) operator with <b>true
   * short-circuiting</b> by transforming the call into a conditional chain at
   * compile time. It returns the first non-null value in the sequence, or the
   * <b>final value</b> (which may be {@code null}) if all preceding values are
   * {@code null}.
   * <p>
   * At runtime, {@code _elvis(a(), b(), c())} is functionally equivalent to:
   * <pre>{@code
   * (tmp = a()) != null ? tmp : (tmp = b()) != null ? tmp : c()
   * }</pre>
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // getDefaultConfig() is ONLY called if findConfig() returns null
   * var cfg = _elvis(findConfig(), getDefaultConfig());
   * }</pre>
   * <p>
   * <b>Transformation Constraints:</b>
   * <p>
   * Passing an empty argument list or attempting array unpacking will result in
   * a compilation error during the Gradle plugin's transformation phase.
   *
   * @param values a sequence of expressions to be evaluated lazily.
   * @param <T>    the inferred common type of the expressions.
   * @return the first non-null value in the sequence, or the final value (which
   * can be {@code null}).
   */
  @SafeVarargs
  public static <T> T _elvis(T... values) {
    unenchanted();
    return values[0];
  }

  public <T> T _void(Runnable fn) {
    unenchanted();
    return null;
  }

  public <T> T _run(Supplier<T> fn) {
    unenchanted();
    return fn.get();
  }

  public <T> T _also(T object, Consumer<T> fn) {
    unenchanted();
    return object;
  }

  public <T, R> R _with(T object, Function<T, R> fn) {
    unenchanted();
    return fn.apply(object);
  }

  public <T> T _safe(T expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Byte _safe(byte expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Short _safe(short expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Integer _safe(int expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Long _safe(long expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Float _safe(float expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Double _safe(double expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Character _safe(char expr) {
    return (boolean) unenchanted() ? expr : null;
  }

  public Boolean _safe(boolean expr) {
    return (boolean) unenchanted() ? expr : null;
  }
}
