# Enchanted Java

## Usage

```kotlin
// settings.gradle.kts

pluginManagement {
  repositories {
    maven("https://maven.yqloss.net") // The plugin
  }
}
```

```kotlin
// build.gradle.kts

plugins {
  id("net.yqloss.enchanted-java-plugin") version PLUGIN_VERSION
}

repositories {
  maven("https://maven.yqloss.net") // The library
}

dependencies {
  compileOnly("net.yqloss:enchanted-java-library:$LIBRARY_VERSION")
}
```

```java
// *.java

import static yqloss.E.*;
```

If you want to use hot reload, add the following JVM arguments:

```text
-javaagent:"path/to/your/enchanted-java-plugin.jar"
-Dyqloss.enchant.prefixes=com.example.package1.,com/example/package2/
```

Only classes with the specified prefixes will be transformed.

The latest version numbers can be checked here:

* [The library](library/build.gradle.kts) `version = "xxx"`
* [The plugin](plugin/build.gradle.kts) `version = "xxx"`

## Goals

* No runtime libraries.
* Can compile with vanilla Javac by introducing only one compileOnly library.
* Transformations are all inside one class. No reading other classes. No
  generating extra classes.

## Implemented Features

* True (`_all`) and false (`_any`) constants for prettier chained logical
  expressions
* Empty body placeholder: `_pass()`
* Never (unreachable) as expression: `_never()` `_never_()`
* Throw as expression: `_throw(exception)` `_throw_(exception)`
  `$throw(exception)`
* Return as expression with finally and synchronized support: `_return()`
  `_return(value)` `_return_()` `_return_(value)` `$return(value)`
* Elvis operator with short circuit: `$elvis(value1, value2, ...)` or `$`
* Scope functions: `_void`, `_run`, `_also`, `_with`, `$also`, `$with`
* Safe member access: `$safe(getUser().getName())` or `$`
* Unchecked cast without warning: `_cast(value)`
* Compile-time random UUID for identifiers: `_id`

For the bottom type, there are currently two implementations:

* `_func`: returns `<T> T` to use in expressions
* `_func_`: returns `RuntimeException` so as to `throw _func_()`

`$` is used to note "null-safety related".

And you can also combine them as you want!

```java
var input = $($(getOptionalInput().getFieldA()), _return(null));
```

## TODO

* Not coming soon...

## License

[MIT LICENSE](LICENSE)
