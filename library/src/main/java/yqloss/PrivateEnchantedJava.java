package yqloss;

import java.util.UUID;
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
  protected static boolean internalFalse = false;

  protected static int internal0 = 0;

  protected static void unpure() {
    internalFalse = false;
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
  public static boolean _any = false;

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
  public static boolean _all = true;

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
  public static boolean _switch = false;

  public static UUID _id = UUID.randomUUID();

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
    unpure();
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
    unpure();
    throw new UnenchantedException(throwable.hashCode());
  }

  /**
   * Throws a {@link Throwable}, designed to be used as
   * {@code throw _throw_(e)}. While the {@code throw} keyword satisfies Java's
   * reachability analysis, the Gradle plugin transforms the {@code _throw_()}
   * call into a direct {@code athrow} instruction. Execution is terminated by
   * the method call itself, meaning the outer {@code throw} is never executed.
   * This approach allows any exception to be thrown without adding it to the
   * method's {@code throws} signature.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Sneaky throw: throwing a checked exception without a throws clause
   * public void runTask() {
   *     throw _throw_(new Exception("Checked exception"));
   * }
   * }</pre>
   *
   * @param throwable the exception to be thrown.
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException _throw_(Throwable throwable) {
    unpure();
    throw new UnenchantedException(throwable.hashCode());
  }

  /**
   * Conditionally throws an exception if the provided {@code throwable} is
   * non-null.
   * <p>
   * This method acts as a conditional control-flow anchor. If the argument is
   * {@code null}, the method performs no operation and returns {@code null},
   * allowing the execution to continue. If the argument is <b>non-null</b>, the
   * Gradle plugin transforms this call into a direct {@code athrow}
   * instruction, terminating the execution immediately.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // If validator returns an error, throw it; otherwise, continue to process data
   * $throw(validator.getError(input));
   * process(input);
   * }</pre>
   * <p>
   *
   * @param throwable the potential exception to throw; if {@code null},
   *                  execution continues.
   * @param <T>       the inferred type to allow use in any expression.
   * @return {@code null} if the input is {@code null}.
   */
  public static <T> T $throw(Throwable throwable) {
    unpure();
    if (throwable != null) throw new UnenchantedException();
    return null;
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
    unpure();
    throw new UnenchantedException();
  }

  /**
   * Marks a code path as unreachable, designed to be used as
   * {@code throw _never_()}. While the {@code throw} keyword satisfies Java's
   * reachability analysis, the Gradle plugin transforms the {@code _never_()}
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
   * throw _never_();
   * }</pre>
   *
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException _never_() {
    unpure();
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
    unpure();
    throw new UnenchantedException();
  }

  /**
   * Performs a return from the current method, designed to be used as
   * {@code throw _return_()}. While the {@code throw} keyword satisfies Java's
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
   *   if (isDone()) throw _return_();
   *   // ... rest of the logic
   * }
   * }</pre>
   *
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException _return_() {
    unpure();
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
    unpure();
    throw new UnenchantedException();
  }

  /**
   * Performs a return with a value from the current method, designed to be used
   * as {@code throw _return_(value)}. The Gradle plugin transforms this call
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
   *     if (flag) throw _return_(1); // Exact match for Integer wrapper
   *     return 0;
   * }
   * }</pre>
   *
   * @param value the value to be returned by the parent method.
   * @return a placeholder {@link RuntimeException} to satisfy Java's
   * {@code throw} syntax.
   */
  public static RuntimeException _return_(Object value) {
    unpure();
    throw new UnenchantedException();
  }

  /**
   * Conditionally performs a return from the current method if the provided
   * {@code value} is non-null.
   * <p>
   * If the {@code value} is {@code null}, this method returns {@code null} and
   * the execution of the parent method continues. If the {@code value} is
   * <b>non-null</b>, the Gradle plugin replaces this call with a return
   * instruction, effectively exiting the parent method with that value. It
   * provides full support for {@code finally} blocks and {@code synchronized}
   * monitors.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Early exit if a cached value is found, otherwise compute it
   * $return(cache.get(key));
   * var newValue = compute(key);
   * }</pre>
   * <p>
   * <b>Transformation Logic:</b>
   * <p>
   * The Gradle plugin injects a null-check and a conditional return. The same
   * type-casting and {@code void} ignoring rules as {@link #_return(Object)}
   * apply.
   *
   * @param value the value to potentially return; if {@code null}, execution
   *              continues.
   * @param <T>   the inferred type to allow use in any expression.
   * @return {@code null} if the input is {@code null}.
   */
  public static <T> T $return(Object value) {
    unpure();
    if (value != null) throw new UnenchantedException();
    return null;
  }

  /**
   * Mimics the null-coalescing (Elvis) operator with <b>true
   * short-circuiting</b> by transforming the call into a conditional chain at
   * compile time. It returns the first non-null value in the sequence, or the
   * <b>final value</b> (which may be {@code null}) if all preceding values are
   * {@code null}.
   * <p>
   * At runtime, {@code $elvis(a(), b(), c())} is functionally equivalent to:
   * <pre>{@code
   * (tmp = a()) != null ? tmp : (tmp = b()) != null ? tmp : c()
   * }</pre>
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // getDefaultConfig() is ONLY called if findConfig() returns null
   * var cfg = $elvis(findConfig(), getDefaultConfig());
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
  public static <T> T $elvis(T... values) {
    unpure();
    return values[internal0];
  }

  /**
   * Alias for {@code $elvis}.
   *
   * @see #$elvis
   */
  @SafeVarargs
  public static <T> T $(T... values) {
    unpure();
    return values[internal0];
  }

  /**
   * Executes a block of code and returns {@code null}. The Gradle plugin
   * replaces this call with a direct invocation of {@link Runnable#run()}
   * followed by an {@code ACONST_NULL} instruction. This is useful for
   * executing side effects within an expression context where a return value is
   * required.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Log a message and return null in a ternary expression
   * var result = (data != null) ? data : _void(() -> logger.warn("Data is null"));
   * }</pre>
   *
   * @param fn  the code block to execute.
   * @param <T> the inferred type (always {@code null} at runtime).
   * @return {@code null} after executing the block.
   */
  public static <T> T _void(Runnable fn) {
    unpure();
    fn.run();
    return null;
  }

  /**
   * Executes a block of code and returns its result, mimicking Kotlin's
   * {@code run}. The Gradle plugin replaces this call with a direct invocation
   * of {@link Supplier#get()}. It allows for grouping multiple statements into
   * a single expression.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * var result = _run(() -> {
   *     var temp = compute();
   *     return temp.isValid() ? temp : defaultVal;
   * });
   * }</pre>
   *
   * @param fn  the code block that supplies the result.
   * @param <T> the type of the result.
   * @return the value returned by the supplier.
   */
  public static <T> T _run(Supplier<T> fn) {
    unpure();
    return fn.get();
  }

  /**
   * Passes the given object to a consumer and returns the object itself,
   * mimicking Kotlin's {@code also}. The Gradle plugin optimizes this using
   * {@code SWAP} and {@code DUP_X1} instructions to invoke
   * {@link Consumer#accept(Object)} while preserving the original object on the
   * stack.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Initialize an object and use it immediately
   * return _also(new User(), u -> u.setName("Alice"));
   * }</pre>
   *
   * @param object the object to be operated upon.
   * @param fn     the action to perform on the object.
   * @param <T>    the type of the object.
   * @return the original {@code object}.
   */
  public static <T> T _also(T object, Consumer<T> fn) {
    unpure();
    fn.accept(object);
    return object;
  }

  /**
   * Passes the given object to a function and returns the transformed result,
   * mimicking Kotlin's {@code let} or {@code with}. The Gradle plugin replaces
   * this call with a direct invocation of {@link Function#apply(Object)}.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Build a string with StringBuilder
   * var string = _with(new StringBuilder(), sb -> {
   *   sb.append("username: ");
   *   sb.append(user.name);
   *   return sb.toString();
   * });
   * }</pre>
   *
   * @param object the object to be transformed.
   * @param fn     the function that performs the transformation.
   * @param <T>    the type of the input object.
   * @param <R>    the type of the result.
   * @return the result of applying the function to the object.
   */
  public static <T, R> R _with(T object, Function<T, R> fn) {
    unpure();
    return fn.apply(object);
  }

  /**
   * Performs a null-safe side effect on an object, mimicking Kotlin's
   * {@code ?.also}.
   * <p>
   * If the {@code object} is {@code null}, this method returns {@code null}
   * immediately without invoking the consumer. If it is non-null, the
   * {@code fn} is executed with the object as its argument, and the original
   * object is returned. The Gradle plugin implements this via a
   * short-circuiting jump in the bytecode.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Only logs and updates the timestamp if the session is not null
   * var activeSession = $also(getSession(), s -> {
   *     logger.info("Session found: " + s.getId());
   *     s.setLastAccess(System.currentTimeMillis());
   * });
   * }</pre>
   *
   * @param object the nullable object to operate upon.
   * @param fn     the action to perform if the object is not null.
   * @param <T>    the type of the object.
   * @return the original {@code object}, or {@code null} if the input was
   * {@code null}.
   */
  public static <T> T $also(T object, Consumer<T> fn) {
    unpure();
    if (object != null) fn.accept(object);
    return object;
  }

  /**
   * Performs a null-safe transformation on an object, mimicking Kotlin's
   * {@code ?.let}.
   * <p>
   * If the {@code object} is {@code null}, this method returns {@code null}
   * immediately without invoking the function. Otherwise, it returns the result
   * of applying the function to the object. This is a placeholder for a Gradle
   * plugin transformation that injects a null-check and jump in the bytecode.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Safely convert a nullable string to its length, or get 0 via $elvis
   * var length = $elvis($with(getName(), String::length), 0);
   *
   * // Transform a nullable entity to a DTO
   * var dto = $with(userRepository.findById(id), UserDTO::fromEntity);
   * }</pre>
   *
   * @param object the nullable object to be transformed.
   * @param fn     the function to apply if the object is not null.
   * @param <T>    the type of the input object.
   * @param <R>    the type of the result.
   * @return the transformed result, or {@code null} if the input was
   * {@code null}.
   */
  public static <T, R> R $with(T object, Function<T, R> fn) {
    unpure();
    return object == null ? null : fn.apply(object);
  }

  /**
   * Evaluates an expression with implicit safe navigation and safe casting.
   * <p>
   * This method acts as a marker for a Gradle plugin transformation. The plugin
   * traces backwards from this call and injects safety checks for all
   * instructions evaluating at the <b>same stack depth</b>:
   * <ul>
   *   <li><b>Safe Navigation:</b> A {@code null} check is injected after reference-producing
   *       instructions. If a reference is {@code null}, the expression short-circuits,
   *       preventing {@link NullPointerException}. Note that this includes single-argument static
   *       methods at this depth.</li>
   *   <li><b>Safe Casting:</b> Any type cast ({@code CHECKCAST}) is prefixed with
   *       an {@code instanceof} check. If the cast is invalid, it short-circuits
   *       instead of throwing a {@link ClassCastException}.</li>
   * </ul>
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Deep safe navigation without multiple ?. operators
   * var city = $safe(user.getAddress().getCity().getName());
   *
   * // Safe casting (evaluates to null if 'obj' is not a String)
   * var text = $safe((String) obj);
   * }</pre>
   * <p>
   * <b>Transformation Constraints:</b>
   * <p>
   * <ul>
   *   <li>{@code $safe} (or its alias {@code $}) <b>cannot</b> be directly nested
   *       inside another {@code $safe}.</li>
   *   <li><b>Instance-accessed static methods:</b> Calling a {@code static} method
   *       via an object instance (e.g., {@code obj.staticMethod()}) alters the expected
   *       bytecode stack layout and is strictly prohibited.</li>
   *   <li><b>Top-level branching:</b> Control flow expressions (such as the ternary
   *       operator {@code ?:} or {@code switch} expressions) cannot be used directly
   *       at the top level of the {@code $safe} expression.</li>
   *   <li>To bypass null-checks for specific operations within the expression,
   *       use {@link #$unsafe(Object)}.</li>
   * </ul>
   *
   * @param expr the expression to evaluate safely.
   * @param <T>  the inferred type of the expression.
   * @return the result of the expression, or {@code null} if a null reference
   * or invalid cast is encountered.
   */
  public static <T> T $safe(T expr) {
    unpure();
    return internalFalse ? expr : null;
  }

  /**
   * Alias for {@code $safe}.
   *
   * @see #$safe
   */
  public static <T> T $(T expr) {
    unpure();
    return internalFalse ? expr : null;
  }

  /**
   * Opts a sub-expression out of the safety transformation applied by an
   * enclosing {@code #$safe} block.
   * <p>
   * When the Gradle plugin injects null-checks for operations at the target
   * stack depth, wrapping an operation in {@code $unsafe} instructs the plugin
   * to skip the null-check for that specific operation. The sub-expression will
   * be evaluated normally and can throw standard exceptions.
   * <p>
   * <b>Strict Placement Constraint:</b><br>
   * {@code $unsafe} must <b>only</b> be used in positions where the plugin
   * would normally inject a safety check (i.e., operations sharing the exact
   * stack depth being analyzed by the enclosing {@code $safe}). If used in
   * nested positions that are not targeted by the tracer, the call will not be
   * processed and removed, resulting in a <b>compilation error</b> during the
   * checker phase.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Normally, StaticApi.fetch() would be null-checked before .process().
   * // $unsafe skips this check, allowing NPE if fetch() returns null.
   * var result = $safe($unsafe(StaticApi.fetch()).process());
   * }</pre>
   *
   * @param expr the sub-expression to execute without injected safety checks.
   * @param <T>  the inferred type of the expression.
   * @return the result of the expression.
   * @see #$safe(Object)
   */
  public static <T> T $unsafe(T expr) {
    unpure();
    return expr;
  }

  /**
   * Suppresses compiler warnings in generic contexts where type safety is
   * guaranteed by logic but cannot be formally proven by the Java compiler.
   * <p>
   * This method is a placeholder for a Gradle plugin transformation that
   * entirely removes the method call from the bytecode. It serves exclusively
   * to provide a clean, expression-level alternative to
   * {@code @SuppressWarnings} or messy manual casting.
   * <p>
   * Since the Java compiler automatically inserts a {@code CHECKCAST}
   * instruction at the call site of generic methods to ensure the result
   * matches the inferred type {@code T}, removing this call allows the native
   * casting mechanism to take over with <b>zero runtime overhead</b>. Use this
   * method only when the type conversion is known to be safe.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Suppresses warnings when casting from a raw type or Object in a chain
   * Element element = _cast(list.get(0));
   *
   * // Cleanly handle generic mapped values without @SuppressWarnings blocks
   * return _cast(context.get("key"));
   * }</pre>
   *
   * @param object the object to be treated as type {@code T}.
   * @param <T>    the target generic type inferred from the context.
   * @return the object, treated as type {@code T} by the compiler.
   */
  public static <T> T _cast(Object object) {
    return (T) object;
  }
}
