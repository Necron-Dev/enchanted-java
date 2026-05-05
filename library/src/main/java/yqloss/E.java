package yqloss;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * One of the main entry points for the Enchanted Java public API.
 * <p>
 * It is <b>recommended</b> to use a static import for this class:
 * <pre>{@code import static yqloss.E.*; }</pre>
 * unless you need to explicitly specify generic parameters.
 */
public final class E {
  protected static boolean internalFalse = false;

  protected static int internal0 = 0;

  protected static void unpure() {
    internalFalse = false;
  }

  protected static <T> T unknown() {
    return (T) new Object();
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

  /**
   * Provides a compile-time token for generating a unique {@link UUID} constant
   * at each reference site. The Gradle plugin replaces every access to this
   * field with a read from a newly generated internal static UUID field on the
   * transformed class. That generated field is initialized with a random UUID
   * during transformation, giving each {@code _id} usage its own stable runtime
   * value.
   */
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
   * <b>Usage Constraint:</b> This variant is intended for methods
   * with a {@code void} or reference ({@code null}) return type.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Using return as an expression to exit a method early
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
  public static <T> T _run(Supplier<? extends T> fn) {
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
  public static <T> T _also(T object, Consumer<? super T> fn) {
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
  public static <T, R> R _with(T object, Function<? super T, ? extends R> fn) {
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
  public static <T> T $also(T object, Consumer<? super T> fn) {
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
  public static <T, R> R $with(T object, Function<? super T, ? extends R> fn) {
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

  /**
   * Computes a value at transformation time and embeds it as a generated static
   * constant field in the transformed class.
   * <p>
   * This method is a compile-time token. The Gradle plugin replaces each call
   * with a read from a newly generated internal static field. During
   * transformation, the supplied lambda is executed once, its result is
   * serialized into that generated field, and the original {@code _const} call
   * is removed. Even if the expression produces a different value on each
   * execution, such as {@link UUID#randomUUID()}, the generated field keeps the
   * value computed during transformation and will not change at runtime.
   * <p>
   * <b>Usage Constraint:</b> the supplier must be a lambda without captures.
   * Capturing local variables, instance state, or otherwise producing a lambda
   * that cannot be resolved to a no-argument static lambda body will fail
   * during the plugin transformation phase.
   * <p>
   * <b>Supported Values:</b>
   * <ul>
   *   <li>{@code null}</li>
   *   <li>boxed primitive values: {@link Boolean}, {@link Byte},
   *       {@link Short}, {@link Integer}, {@link Long}, {@link Float},
   *       {@link Double}, and {@link Character}</li>
   *   <li>{@link String}; long strings are split into loadable chunks and
   *       rebuilt with {@link StringBuilder}</li>
   *   <li>primitive arrays</li>
   *   <li>{@code byte[]}; bytes are stored as a Base64 string and decoded when
   *       the generated field is initialized</li>
   *   <li>object arrays whose elements are themselves supported values</li>
   * </ul>
   * <p>
   * Unsupported values fail during transformation with an
   * {@link UnsupportedOperationException}.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * private static final String NAME = _const(() -> "enchanted");
   *
   * private static final String BUILD_ID = _const(() -> UUID.randomUUID().toString());
   *
   * private static final int[] TABLE = _const(() -> new int[] {1, 1, 2, 3, 5});
   * }</pre>
   *
   * @param expr the capture-free supplier to execute during transformation.
   * @param <T>  the type of the computed constant value.
   * @return the computed value, loaded from the generated static field after
   * transformation.
   */
  public static <T> T _const(Supplier<? extends T> expr) {
    unpure();
    return expr.get();
  }

  /**
   * Reads a compile-time property token by name.
   * <p>
   * The Gradle plugin replaces each call with a read from a generated static
   * field initialized from the plugin's property map. The {@code name} argument
   * must be a string constant, and the named property must exist in the
   * configured properties; otherwise transformation fails.
   * <p>
   * In Gradle, configure the plugin with {@code enchantedJava { properties }},
   * where {@code properties} is a {@code MapProperty<String, Object>}:
   * <pre>{@code
   * // build.gradle.kts
   * enchantedJava {
   *   properties.put("apiKey", "sk_test_123")
   *   properties.put("endpointUrl", "https://api.example.com")
   *   properties.put("retryCount", 3)
   *   properties.put("enabled", true)
   * }
   * }</pre>
   * <p>
   * Read them from Java source with {@code _property(name)}:
   * <pre>{@code
   * var apiKey = _property("apiKey");
   * var endpointUrl = _property("endpointUrl");
   * var retryCount = _property("retryCount");
   * var enabled = _property("enabled");
   * }</pre>
   *
   * @param name the property name to read.
   * @param <T>  the inferred type of the property value.
   * @return the property value provided by the plugin at transformation time.
   */
  public static <T> T _property(String name) {
    unpure();
    return unknown();
  }

  /**
   * Marks a varargs overload as the named-argument entry point for another
   * method or constructor in the same class.
   * <p>
   * The Gradle plugin replaces the body of a method containing this token with
   * bytecode that reads an array of argument records, fills the backing
   * method's parameters by name, computes missing parameters annotated with
   * {@link Default}, and delegates to the backing method.
   * <p>
   * Behavior differs between normal and strict modes:
   * <ul>
   *   <li><b>Normal mode</b> ({@code _defaultArgs} / {@code _defaultArgs_}):
   *       duplicate arguments use the last value; vararg items whose names map
   *       to positional parameters are ignored; vararg items whose names do not
   *       exist fail with {@link IllegalArgumentException}.</li>
   *   <li><b>Strict mode</b> ({@code _defaultArgsStrict} /
   *       {@code _defaultArgsStrict_}): duplicate arguments fail;
   *       vararg items whose names map to positional parameters, or whose names
   *       do not exist, fail with {@link IllegalArgumentException}.</li>
   * </ul>
   * In both modes, {@code null} argument records and missing required arguments
   * fail with {@link IllegalArgumentException}.
   * <p>
   * Default values for optional parameters can be provided in three ways:
   * <ol>
   *   <li>A {@code @Default("value")} constant on the parameter (simple
   *       literals only).</li>
   *   <li>An inline default via {@link #_default} in the
   *       backing method body. The parameter must carry {@code @Default}
   *       (without a value).</li>
   *   <li>A separate member provider (field or method) named
   *       {@code backingName$parameterName}. The parameter must carry
   *       {@code @Default} (without a value).</li>
   * </ol>
   * A {@code @Default("value")} constant and an inline default for the same
   * parameter cannot coexist.
   * <p>
   * The generated overload must take its <b>last</b> parameter as a varargs
   * whose element type is the argument record class. Any parameters before the
   * varargs are <b>positional</b>: they are matched by name to backing-method
   * parameters and passed directly, without needing an argument record. A
   * positional parameter's name is read from debug local-variable information,
   * or from {@link Name} on the overload's parameter when debug information is
   * unavailable or a different name is desired. The type of each positional
   * parameter must exactly match the corresponding backing-method parameter's
   * type. Positional parameters that do not match any backing-method parameter
   * name cause a transformation error.
   * <p>
   * The argument record class must be public and expose the canonical
   * constructor and accessors for {@code name} and {@code value}:
   * <pre>{@code
   * public class ExampleArg {
   *   public ExampleArg(String name, Object value);
   *   public String name();
   *   public Object value();
   * }
   *
   * public record ExampleRecordArg(String name, Object value) {}
   * }</pre>
   * A special unpacking form is also supported: an item shaped as
   * {@code (null, Arg[])} expands the nested array into the current vararg
   * stream. Unpacking cannot be nested.
   * <p>
   * The backing method is found by name. For a varargs method named
   * {@code f(Arg... args)}, the backing method is normally {@code f(...)}. If
   * the generated overload or backing method uses a different Java name,
   * annotate it with {@link Name @Name("logicalName")}. Parameter names are
   * read from debug local-variable information, or from {@link Name} on each
   * parameter when debug information is unavailable or a different public
   * argument name is desired.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * public class Main {
   *   public record Arg(String name, Object value) {}
   *
   *   public static void f(float a, @Default short b) {
   *     System.out.println(a);
   *     System.out.println(b);
   *   }
   *
   *   // Default value provider for parameter b. It may depend on already-filled
   *   // parameters by accepting parameters with matching names and types.
   *   public static short f$b(float a) {
   *     return (short) a;
   *   }
   *
   *   // Equivalent inline form using _default:
   *   // public static void f(float a, @Default short b) {
   *   //   _default(b, (short) a);
   *   // }
   *
   *   // All named arguments via varargs:
   *   public static void f(Arg... args) {
   *     _defaultArgs();
   *   }
   *
   *   // Mixed: 'a' is passed positionally, 'b' via named argument:
   *   public static void f(float a, Arg... args) {
   *     _defaultArgs();
   *   }
   *
   *   public static Arg _a(float value) {
   *     return _arg();
   *   }
   *
   *   public static Arg _b(short value) {
   *     return _arg();
   *   }
   *
   *   public static void main(String... args) {
   *     f(_a(1));              // b defaults to (short) a
   *     f(_a(1), _b((short) 2));
   *     f(1.0f, _b((short) 2)); // a passed positionally, b via name
   *   }
   * }
   * }</pre>
   *
   * @param <T> the return type of the backing method.
   * @return the value returned by the backing method after transformation.
   * @see #_arg()
   * @see #_default
   * @see Default
   * @see Name
   */
  public static <T> T _defaultArgs() {
    unpure();
    return unknown();
  }

  /**
   * Strict variant of {@link #_defaultArgs()}.
   * <p>
   * Compared to {@code _defaultArgs()}, this variant rejects duplicate
   * arguments, and rejects vararg items whose names map to positional
   * parameters or do not exist.
   *
   * @param <T> the return type of the backing method.
   * @return the value returned by the backing method after transformation.
   * @see #_defaultArgs()
   */
  public static <T> T _defaultArgsStrict() {
    unpure();
    return unknown();
  }

  /**
   * Alias for {@link #_defaultArgs} that is intended for
   * {@code throw _defaultArgs_()} style usage.
   *
   * @see #_arg
   */
  public static RuntimeException _defaultArgs_() {
    unpure();
    return new UnenchantedException();
  }

  /**
   * Alias for {@link #_defaultArgsStrict()} that is intended for
   * {@code throw _defaultArgsStrict_()} style usage.
   * <p>
   * Compared to {@link #_defaultArgs_()}, this variant uses strict behavior:
   * duplicate arguments fail, and vararg items targeting positional or unknown
   * names fail.
   *
   * @see #_defaultArgsStrict()
   * @see #_defaultArgs_()
   */
  public static RuntimeException _defaultArgsStrict_() {
    unpure();
    return new UnenchantedException();
  }

  /**
   * Marks a single-parameter static helper as a named-argument constructor for
   * use with a {@link #_defaultArgs()} overload.
   * <p>
   * The Gradle plugin replaces the helper body with construction of the
   * enclosing class's argument record. The helper must return the argument
   * record type, and must be either:
   * <ul>
   *   <li>a one-parameter method (static or instance), where that parameter is
   *       used as the argument value, or</li>
   *   <li>a zero-parameter instance method, where {@code this} is used as the
   *       argument value.</li>
   * </ul>
   * The generated record value stores the argument name and the boxed selected
   * value.
   * <p>
   * By default, the argument name is the part of the helper method name after
   * the last underscore. For example, {@code _count(int value)} produces an
   * argument named {@code "count"}. Use {@link Name} on the helper method when
   * the Java method name cannot or should not match the public argument name.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * public static void connect(String host, @Default("5432") int port) {
   *   // ...
   * }
   *
   * public static void connect(Arg... args) {
   *   _defaultArgs();
   * }
   *
   * public static Arg _host(String value) {
   *   return _arg();
   * }
   *
   * @Name("port")
   * public static Arg portArg(int value) {
   *   return _arg();
   * }
   *
   * connect(_host("localhost"));
   * connect(_host("localhost"), portArg(15432));
   * }</pre>
   *
   * @param <T> the configured argument record type.
   * @return a generated argument record after transformation.
   * @see #_defaultArgs()
   * @see Name
   */
  public static <T> T _arg() {
    unpure();
    return unknown();
  }

  /**
   * Alias for {@link #_arg} but returns {@link RuntimeException}.
   * <p>
   * It follows the same transformation rules as {@link #_arg()}.
   *
   * @see #_arg
   */
  public static RuntimeException _arg_() {
    unpure();
    return new UnenchantedException();
  }

  /**
   * Marks a backing-method parameter as optional for a {@link #_defaultArgs()}
   * overload and describes how its value is computed when omitted.
   * <p>
   * If {@link #value()} is non-empty, the annotation value is parsed as an
   * in-place constant default. Direct constants are supported only for
   * primitive types, boxed primitive types, {@link String}, and {@code null}
   * for reference types. Supported literals include {@code true}/{@code false},
   * decimal or {@code 0x}-prefixed integer literals with underscores, float and
   * double literals including {@code NaN}, {@code Infinity}, and
   * {@code -Infinity}. Character literals in the form {@code 'x'} are supported
   * for {@code char} and {@link Character}; alternatively, a numeric Unicode
   * code point can be used. String literals in the form {@code 'text'}
   * (single-quoted) have the quotes stripped; otherwise the annotation value is
   * used as-is.
   * <p>
   * If {@link #value()} is empty, the plugin first checks for an inline default
   * expression via {@link #_default} in the backing method. If no inline
   * default is found, the plugin looks for a default value provider named
   * {@code backingName$parameterName}. A {@code @Default("value")} constant and
   * an inline default for the same parameter cannot coexist; the parameter must
   * still carry {@code @Default} (without a value) to mark it as optional.
   * <p>
   * A default value provider may be either a field or a method, and may use
   * {@link Name} to expose that logical provider name when the Java member name
   * differs. A provider for a static backing method must be static; a provider
   * for an instance backing method may be static or instance. The provider type
   * or return type must exactly match the parameter type.
   * <p>
   * Method providers may declare dependencies on other parameters. Dependency
   * parameters are matched by name (or parameter {@link Name}) and exact type,
   * and must already be filled by the caller or by an earlier default provider.
   * Missing dependencies fail with {@link IllegalArgumentException}.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * public static void open(
   *   String host,
   *   @Default("8080") int port,
   *   @Default("true") boolean secure,
   *   @Default String url,
   *   @Default String header
   * ) {
   *   // ...
   * }
   *
   * // Field provider for header.
   * public static String open$header = "Accept: application/json";
   *
   * // Method provider for url; depends on host, port, and secure.
   * public static String open$url(String host, int port, boolean secure) {
   *   return (secure ? "https://" : "http://") + host + ':' + port;
   * }
   * }</pre>
   *
   * @see #_defaultArgs()
   * @see #_default
   * @see Name
   */
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.PARAMETER)
  public @interface Default {
    /**
     * The optional in-place constant default literal. Leave empty to use a
     * field or method provider named {@code backingName$parameterName}.
     *
     * @return the constant literal text, or an empty string to request a member
     * provider.
     */
    String value() default "";
  }

  /**
   * Supplies the logical name used by default-argument and named-argument
   * transformations when the Java source name is unavailable, ambiguous, or not
   * the desired public API name.
   * <p>
   * Supported locations:
   * <ul>
   *   <li>On a {@link #_defaultArgs()} overload: selects the backing method or
   *       constructor logical name.</li>
   *   <li>On a {@link #_defaultArgs()} overload's positional parameters: sets the
   *       public argument name used to match positional parameters to backing-method
   *       parameters by name.</li>
   *   <li>On the backing method or constructor: disambiguates which executable
   *       should receive calls from the generated overload when multiple Java
   *       executables share the same name.</li>
   *   <li>On backing-method parameters: sets public argument names and avoids
   *       requiring debug local-variable information.</li>
   *   <li>On {@link #_arg()} helper methods: sets the argument name stored in the
   *       generated argument record.</li>
   *   <li>On default-value provider methods, provider method parameters, and
   *       provider fields: supplies the logical {@code backingName$parameterName}
   *       or dependency name when the Java member or parameter name differs.</li>
   * </ul>
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * @Name("create")
   * public User(String login, @Name("display-name") @Default String displayName) {
   *   // ...
   * }
   *
   * @Name("create$display-name")
   * private static String defaultDisplayName(@Name("login") String value) {
   *   return value;
   * }
   *
   * @Name("create")
   * public User(@Name("login") String login, Arg... args) {
   *   _defaultArgs();
   * }
   *
   * @Name("display-name")
   * public static Arg displayName(String value) {
   *   return _arg();
   * }
   * }</pre>
   */
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
  public @interface Name {
    /**
     * The logical name used by the relevant transformation.
     *
     * @return the transformed public or provider name.
     */
    String value();
  }

  /**
   * Alias for {@code $elvis($safe(a), $safe(b), ...)}.
   *
   * @see #$elvis
   * @see #$safe
   */
  @SafeVarargs
  public static <T> T $$(T... values) {
    unpure();
    return values[internal0];
  }

  /**
   * Specifies an inline default value for a backing-method parameter within a
   * {@link #_defaultArgs()} overload. The Gradle plugin extracts the
   * {@code value} expression and uses it as the default computation for the
   * parameter identified by {@code param}.
   * <p>
   * The first argument must be a <b>direct parameter load</b> (no boxing,
   * arithmetic, or method calls). The plugin uses the loaded local-variable
   * slot to match the argument to the corresponding backing-method parameter by
   * name. The second argument may reference other parameters by name; those
   * references become dependencies that must be filled before the default is
   * computed.
   * <p>
   * This token is removed from the backing method's bytecode by
   * {@code TrimDefaultPass}. A {@code @Default("value")} constant default and
   * an inline default for the same parameter cannot coexist; however, the
   * {@code param} must still carry a {@link Default @Default} annotation
   * (without a value) to mark it as optional.
   * <p>
   * <b>Primitive note:</b> when the parameter is a primitive type but the
   * default expression evaluates to a wider or boxed type, an explicit cast is
   * required. Use the primitive overload (e.g., {@code _default(short, short)})
   * matching the backing-method parameter type and cast the expression
   * accordingly:
   * <pre>{@code
   * public static short f$b(float a) {
   *   return (short) a;  // cast required: float → short
   * }
   *
   * // Equivalent inline form:
   * public static void f(float a, @Default short b) {
   *   _default(a, (short) (a + 1));
   *   //          ^^^^^^^^^^^^^^^^ cast needed: int → short
   * }
   * }</pre>
   * <p>
   * The expression must not be involved in outer control flow (jumps to labels
   * outside the expression). Local control flow within the expression (ternary
   * operators compiled to conditional jumps) is allowed as long as all target
   * labels are within the expression itself.
   *
   * @param param identifies the backing-method parameter this default is for.
   * @param value the expression computing the default value.
   * @see #_defaultArgs()
   * @see Default
   */
  public static <T> void _default(T param, T value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code byte} parameters. Use this
   * overload when the backing-method parameter is {@code byte} to avoid boxing.
   * If the default expression evaluates to a wider type (e.g. {@code int}), an
   * explicit cast to {@code byte} is required.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(byte param, byte value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code short} parameters. Use this
   * overload when the backing-method parameter is {@code short} to avoid
   * boxing. If the default expression evaluates to a wider type (e.g.
   * {@code int}), an explicit cast to {@code short} is required.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(short param, short value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code int} parameters.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(int param, int value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code long} parameters.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(long param, long value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code float} parameters.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(float param, float value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code double} parameters.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(double param, double value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code char} parameters. If the default
   * expression evaluates to a wider type (e.g. {@code int}), an explicit cast
   * to {@code char} is required.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(char param, char value) {
    unpure();
  }

  /**
   * Inline default value overload for {@code boolean} parameters.
   *
   * @see #_default(Object, Object)
   */
  public static void _default(boolean param, boolean value) {
    unpure();
  }

  /**
   * Registers a discard-value receiver for the enclosing method. When active,
   * every expression whose value is discarded within the method is forwarded to
   * the receiver as an {@code (Object value, int index)} pair, where
   * {@code index} is the sequential position of the discarded expression in
   * method body order.
   * <p>
   * The Gradle plugin replaces this call with code that stores the receiver
   * into a local variable, then transforms each {@code POP} or {@code POP2} in
   * the method body: if the receiver is non-null, the discarded value is
   * converted to {@link Object} and passed to
   * {@link ObjIntConsumer#accept(Object, int)} along with its index; if the
   * receiver is {@code null}, the value is simply discarded as usual.
   * <p>
   * Discards performed via {@code $ = value} participate in receiver delivery;
   * discards performed via {@code __ = value} do <b>not</b> — they are always
   * silently dropped regardless of whether a receiver is active.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * public void process(ObjIntConsumer<Object> receiver) {
   *   _receiver(receiver);
   *   computeSomething();  // discarded value → receiver.accept(result, 0)
   *   $ = getSideEffect(); // discarded value → receiver.accept(result, 1)
   *   __ = logDebug();     // silently discarded, not sent to receiver
   * }
   * }</pre>
   *
   * @param receiver the callback that receives each discarded value and its
   *                 expression index; may be {@code null}, in which case all
   *                 discards simply drop the value.
   * @see #$
   * @see #__
   */
  public static void _receiver(ObjIntConsumer<?> receiver) {
    unpure();
  }

  /**
   * Explicitly discards a value, suppressing the "result of … is ignored"
   * compiler warning. The Gradle plugin replaces the field assignment
   * ({@code $ = value}) with a single {@code POP} instruction, removing any
   * trace of the field write at runtime.
   * <p>
   * If a discard receiver is active via {@link #_receiver(ObjIntConsumer)}, the
   * discarded value is <b>forwarded</b> to that receiver along with its
   * sequential expression index. Use {@link #__} to discard a value that should
   * never be observed by a receiver.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * $ = computeSideEffect();    // discard value, suppress warning
   *                             // → with _receiver: receiver.accept(value, n)
   * }</pre>
   *
   * @see #__
   * @see #_receiver(ObjIntConsumer)
   */
  public static Object $ = null;

  /**
   * Explicitly discards a value without forwarding it to a discard receiver.
   * The behavior is identical to {@link #$} except that the discarded value is
   * <b>never</b> delivered to a {@link #_receiver(ObjIntConsumer)}, even when
   * one is active. This is useful for intentionally silencing a warning on a
   * value that should be genuinely ignored rather than inspected.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * __ = logDebug();  // silently discarded, never sent to receiver
   * }</pre>
   *
   * @see #$
   * @see #_receiver(ObjIntConsumer)
   */
  public static Object __ = null;

  /**
   * Prevents the Gradle plugin from inferring the enclosing method as a pure
   * function. The plugin removes this call entirely during transformation; its
   * sole purpose is to mark a method as having side effects or being
   * non-deterministic when the method body would otherwise appear pure.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * public void log(String message) {
   *   _unpure();
   *   System.out.println(message);
   * }
   * }</pre>
   */
  public static void _unpure() {
    unpure();
  }

  /**
   * Computes a value once and reuses it from a generated class-local static
   * cache field.
   * <p>
   * This method is a compile-time token. The Gradle plugin replaces each
   * {@code _cached(() -> expr)} call site with a read from a newly generated
   * internal static {@code Object} field on the transformed class. That field
   * is initialized once in class initialization by invoking the supplied lambda
   * and storing {@link Supplier#get()} result. Subsequent reads return the same
   * cached value.
   * <p>
   * <b>Usage Constraint:</b> the supplier must be a lambda without captures.
   * Capturing locals or instance state will fail during transformation.
   * <p>
   * <b>Examples:</b>
   * <pre>{@code
   * // Each call site gets its own generated cache field in the class.
   * var id = _cached(() -> UUID.randomUUID().toString());
   * }</pre>
   *
   * @param supplier capture-free supplier used to compute the cached value.
   * @param <T>      the cached value type.
   * @return the value loaded from the generated cache field.
   */
  public static <T> T _cached(Supplier<? extends T> supplier) {
    unpure();
    return supplier.get();
  }
}
