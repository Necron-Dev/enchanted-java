package net.yqloss.enchant.library;

/**
 * A placeholder exception used in the method bodies of hooks to satisfy the
 * compiler.
 * <p>
 * If this exception is thrown at runtime, it indicates that the bytecode was
 * not successfully transformed and the hooks were not replaced. To resolve
 * this, consider upgrading the Gradle plugin or
 * <a href="https://ench.yqlo.ss/">reporting an issue</a>.
 */
public class UnenchantedException extends RuntimeException {
}
