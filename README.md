# Enchanted Java

## Goals

* No runtime libraries.
* Can compile with vanilla Javac by introducing only one compileOnly library.
* Transformations are all inside one class. No reading other classes. No
  generating extra classes.

## Implemented Features

```java
import static net.yqloss.enchant.library.EnchantedJava.*;
```

* True (`_all`) and false (`_any`) constants for prettier chained logical
  expressions
* Empty body placeholder: `_pass()`
* Never (unreachable) as expression: `_never()` `$never()`
* Throw as expression: `_throw(exception)` `$throw(exception)`
* Return as expression with finally and synchronized support: `_return()`
  `_return(value)` `$return()` `$return(value)`
* Elvis operator with short circuit: `_elvis(value1, value2, ...)`

For the bottom type, there are currently two implementations:

* `_func`: returns `<T> T` to use in expressions
* `$func`: returns `RuntimeException` so as to `throw $func()`

And you can also combine them as you want!

```java
var input = _elvis(getOptionalInput(), _return(null));
```

## TODO

* Void as expression
* Nullish `?.` operator
* And more!

## License

[MIT LICENSE](LICENSE)
