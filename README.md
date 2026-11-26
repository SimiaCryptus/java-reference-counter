# Java Reference Counter

A toolkit for deterministic, reference-counted resource management in Java. It combines a thread-safe runtime library, reference-aware collection and stream wrappers, object pooling utilities, and an Eclipse JDT–based Gradle plugin (`refcount-autocoder`) that automatically instruments and verifies reference counting in Java source code.

## Overview

Java's garbage collector manages heap memory automatically, but it does not guarantee timely or deterministic release of scarce resources. This becomes critical when working with:

- **Native and off-heap resources** (e.g., GPU buffers, native memory, file descriptors)
- **Large transient allocations** that benefit from immediate release or pooling rather than waiting for GC
- **Deterministic teardown sequences** where parent/child resources must be freed in a well-defined order

The Java Reference Counter toolkit addresses this with four integrated layers:

1. **Core runtime (`refcount-core`)** — `ReferenceCounting` / `ReferenceCountingBase`, `RefUtil`, lifecycle debugging with stack-trace history, `RecycleBin` pooling, and reference-aware wrappers for Java collections, streams, iterators, and functional interfaces.
2. **AST transformation framework (`autocoder-core`)** — Reusable Eclipse JDT–based AST parsing, symbol indexing, and source-rewriting infrastructure for Gradle tasks.
3. **Reference-counting Gradle plugin (`refcount-autocoder`)** — Automatically inserts, optimizes, verifies, and removes reference counting operations (`addRef()`, `freeRef()`, `_free()`, closure capture handling, and collection type rewrites).
4. **Bytecode analyzer & verifier (`refcount-bytecode`)** — ASM-based bytecode verification and reporting for compiled `.class` / `.jar` artifacts and non-Java JVM modules (Kotlin, Scala, Groovy).

## Modules

| Gradle Project | Directory | Description | Documentation |
|---|---|---|---|
| `:refcount-core` | `core/` | Core runtime library: base classes, annotations, `RefUtil`, `RecycleBin`, and reference-counted wrappers | [core/README.md](core/README.md) |
| `:autocoder-core` | `autocoder/` | Base Eclipse JDT AST transformation framework and `AutoCoderTask` base class | [autocoder/README.md](autocoder/README.md) |
| `:refcount-autocoder` | `refcount-autocoder/` | Gradle plugin (`com.simiacryptus.refcount-autocoder`) and CLI for automatic reference-count instrumentation and verification | [refcount-autocoder/README.md](refcount-autocoder/README.md) |
| `:refcount-bytecode` | `refcount-bytecode/` | Gradle plugin (`com.simiacryptus.refcount-bytecode`) and CLI for bytecode analysis and verification of compiled and non-Java modules | [refcount-bytecode/README.md](refcount-bytecode/README.md) |
| `:autocoder-test` | `autocoder-test/` | Test harness (`SimpleGradleProject`) for running autocoder passes against Gradle projects | [autocoder-test/README.md](autocoder-test/README.md) |
| `:refcount-demo` | `demo/` | End-to-end example project instrumented at build time and verified for zero leaks under `DEBUG_LIFECYCLE=true` | [demo/README.md](demo/README.md) |

## Prerequisites & Building

- **JDK 21** (configured via Gradle Java toolchain in `build.gradle.kts`)
- **Gradle Wrapper** (`./gradlew` / `gradlew.bat`)

Build all modules and run the full JUnit 5 test suite with aggregated JaCoCo code coverage:

```bash
# Compile and test all subprojects
./gradlew build

# Run tests only
./gradlew test

# Run verification including aggregated JaCoCo coverage report
./gradlew check
```

Aggregated coverage reports are generated at `build/reports/jacoco/testCodeCoverageReport/html/index.html`.

## Getting Started

### Adding the Dependency

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("com.simiacryptus:refcount-core:2.1.0")
}
```

**Gradle (Groovy DSL):**

```groovy
dependencies {
    implementation 'com.simiacryptus:refcount-core:2.1.0'
}
```

**Maven:**

```xml
<dependency>
    <groupId>com.simiacryptus</groupId>
    <artifactId>refcount-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Creating Reference-Counted Objects

Extend `ReferenceCountingBase` to create objects with thread-safe reference counting. New instances start with a reference count of `1`.

```java
import com.simiacryptus.ref.lang.MustCall;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

public class MyResource extends ReferenceCountingBase {
    private byte[] data;

    public MyResource(int size) {
        this.data = new byte[size];
    }

    public void doWork() {
        assertAlive(); // Throws LifecycleException if already freed
        // ... use data
    }

    @Override
    public MyResource addRef() {
        return (MyResource) super.addRef();
    }

    @MustCall
    @Override
    protected void _free() {
        // Invoked once when the reference count drops to 0
        data = null;
        super._free();
    }
}
```

### Basic Lifecycle Pattern

```java
// Created with refcount = 1
MyResource resource = new MyResource(1024);
try {
    // Share ownership with another consumer (refcount = 2)
    MyResource shared = resource.addRef();
    try {
        shared.doWork();
    } finally {
        shared.freeRef(); // refcount = 1
    }
} finally {
    resource.freeRef(); // refcount = 0 -> _free() is invoked
}
```

For generic types, arrays, `Map.Entry`, or `Optional` values where the runtime type may or may not implement `ReferenceCounting`, use `RefUtil`:

```java
import com.simiacryptus.ref.lang.RefUtil;

T copy = RefUtil.addRef(value);
try {
    // use copy
} finally {
    RefUtil.freeRef(copy);
}
```

### Using Reference-Counted Collections and Streams

`com.simiacryptus.ref.wrappers` provides reference-aware counterparts to standard Java collections, streams, iterators, and functional types:

```java
import com.simiacryptus.ref.wrappers.RefArrayList;

RefArrayList<MyResource> list = new RefArrayList<>();
try {
    list.add(new MyResource(100)); // Collection takes ownership of the passed reference

    MyResource item = list.get(0); // Returns a newly incremented reference
    try {
        item.doWork();
    } finally {
        item.freeRef();
    }
} finally {
    list.freeRef(); // Frees the list and all contained elements
}
```

Available wrapper families include:
- **Lists:** `RefArrayList`, `RefLinkedList`, `RefList`, `RefAbstractList`, `RefLists`
- **Maps:** `RefHashMap`, `RefLinkedHashMap`, `RefTreeMap`, `RefConcurrentHashMap`, `RefMap`, `RefSortedMap`, `RefNavigableMap`, `RefMaps`, `RefEntry`
- **Sets:** `RefHashSet`, `RefTreeSet`, `RefSet`, `RefSortedSet`, `RefNavigableSet`
- **Queues & Deques:** `RefLinkedBlockingQueue`, `RefConcurrentLinkedDeque`, `RefBlockingQueue`, `RefQueue`, `RefDeque`
- **Streams & Collectors:** `RefStream`, `RefIntStream`, `RefDoubleStream`, `RefLongStream`, `RefCollectors`, `RefStreamSupport`
- **Iterators & Spliterators:** `RefIterator`, `RefListIterator`, `RefPrimitiveIterator`, `RefSpliterator`, `RefSpliterators`
- **Functional & Concurrency:** `RefFunction`, `RefBiFunction`, `RefConsumer`, `RefPredicate`, `RefSupplier`, `RefComparator`, `RefAtomicReference`, `RefThreadLocal`, `RefWeakReference`

See [core/README.md](core/README.md) for the full API reference.

## Automatic Instrumentation (`refcount-autocoder`)

Manually writing `addRef()` and `freeRef()` calls across a large codebase is tedious and error-prone. The `refcount-autocoder` Gradle plugin parses your Java sources with Eclipse JDT (with full type-binding resolution) and automatically inserts, optimizes, or verifies reference counting code.

### Gradle Plugin Setup

Apply the plugin and optionally configure the `refcountAutocoder` extension:

```kotlin
plugins {
    java
    id("com.simiacryptus.refcount-autocoder") version "2.1.0"
}

refcountAutocoder {
    modifyAPI.set(false)    // Also rewrite standard Java collections to Ref* wrappers
    parallel.set(false)     // Process compilation units in parallel
    logException.set(true)  // Print detailed errors to stderr before rethrowing
    force.set(false)        // Run even if refcount-core is not detected in dependencies
}
```

### Available Gradle Tasks

All tasks are registered under the `refcount` task group:

| Task | Description |
|---|---|
| `refcountInsert` | Inserts `addRef()`, `freeRef()`, `_free()`, closure instrumentation, and optimizes redundant ref ops |
| `refcountRemove` | Removes inserted reference counting code and inlines temporary variables |
| `refcountVerify` | Verifies method calls, assignments, fields, closures, and local variables for reference leaks or misuse |
| `refcountCheck` | Validates the project's type hierarchy for reference counting rules |
| `refcountUpdate` | Annotates missing `@RefAware` attributes on method calls and runs verification |
| `refcountModifyAPI` | Converts standard Java collection types (`List`, `Map`, `Set`, `Stream`, etc.) to `Ref*` wrappers |
| `refcountRevertAPI` | Reverts `Ref*` collection wrappers back to standard Java types |
| `refcountAnnotate` | Adds `@RefAware` annotations to reference-counted method parameters |
| `refcountTemp` | Removes `@RefAware` type annotations |
| `refcountPrintAST` | Prints the Eclipse JDT AST of project Java source files (debugging aid) |

Example invocation:

```bash
./gradlew refcountInsert -DmodifyAPI=true
./gradlew refcountVerify
```

### Before & After Example

Given a simple container class (from `demo`):

```java
public class SimpleContainer extends ReferenceCountingBase {
  @Nullable
  public BasicType value;

  public SimpleContainer(BasicType value) {
    this.value = value;
  }

  @Override
  public void _free() {
    value = null;
    super._free();
  }
}
```

`refcountInsert` automatically instruments field assignments, parameter cleanup, `_free()`, and a covariant `addRef()` override:

```java
public class SimpleContainer extends ReferenceCountingBase {
  @Nullable
  public BasicType value;

  public SimpleContainer(BasicType value) {
    {
      BasicType temp_03_0002 = value == null ? null : value.addRef();
      if (null != this.value)
        this.value.freeRef();
      this.value = temp_03_0002 == null ? null : temp_03_0002.addRef();
      if (null != temp_03_0002)
        temp_03_0002.freeRef();
    }
    if (null != value)
      value.freeRef();
  }

  @Override
  public void _free() {
    if (null != value)
      value.freeRef();
    value = null;
    super._free();
  }

  @Override
  public SimpleContainer addRef() {
    return (SimpleContainer) super.addRef();
  }
}
```

You can also run `com.simiacryptus.ref.Insert` directly from a Gradle `JavaExec` task to instrument sources into a build directory without modifying tracked source files (see [demo/build.gradle.kts](demo/build.gradle.kts) and [refcount-autocoder/README.md](refcount-autocoder/README.md)).

## Key Classes, Interfaces & Annotations

### Core Runtime (`com.simiacryptus.ref.lang`)

| Type | Purpose |
|---|---|
| `ReferenceCounting` | Core interface: `addRef()`, `freeRef()`, `isFreed()`, `currentRefCount()`, `assertAlive()`, `tryAddRef()`, `detach()` |
| `ReferenceCountingBase` | Thread-safe abstract base class with atomic refcount, GC leak logging, and stack-trace diagnostics |
| `RefUtil` | Null-safe static helpers for `ReferenceCounting`, arrays, `Map.Entry`, `Optional`, and dynamic `wrapInterface(...)` proxies |
| `RecycleBin<T>` | Size-bucketed object pool with built-in `DOUBLES` and `FLOATS` pools, background purging, and `StackCounter` profiling |
| `RefLazyVal<T>` | Reference-counted lazy value supplier that frees its cached value on `_free()` |
| `ReferenceWrapper<T>` | Wrapper pairing an arbitrary object with an idempotent destructor callback |
| `LifecycleException` | Unchecked exception thrown on use-after-free (`assertAlive()`), double-free, or `assertFreed()` failure |

### Annotations

| Annotation | Purpose |
|---|---|
| `@RefAware` | Marks parameters, return values, or types that participate in reference ownership transfer |
| `@RefIgnore` | Excludes a class, method, or parameter from autocoder instrumentation and verification |
| `@MustCall` | Marks methods (such as `_free()`) that overriding subclasses must invoke via `super` |

## Lifecycle Debugging & Configuration

`ReferenceCountingBase` can record creation, `addRef()`, and `freeRef()` stack traces to diagnose leaks and use-after-free bugs:

```java
// Watch a specific object instance (when WATCH_ENABLE=true)
resource.watch();

// Inspect full lifecycle history (creation, addRef, freeRef stack traces)
CharSequence report = ReferenceCountingBase.referenceReport(resource, true);

// Assert in tests that an object was properly freed
resource.assertFreed();
```

### JVM System Properties

`RefSettings` reads configuration from **JVM system properties** (via `System.getProperty`):

| System Property | Description | Default |
|---|---|---|
| `DEBUG_LIFECYCLE` | Enable full lifecycle stack-trace tracking for all `ReferenceCountingBase` subclasses | `false` |
| `WATCH_ENABLE` | Allow `watch()` to record `addRef()` / `freeRef()` stack traces | `true` |
| `WATCH_CREATE` | Capture creation stack traces on every `ReferenceCountingBase` allocation | `false` |
| `DOUBLE_CACHE_MODE` | Reference strength for `RecycleBin.DOUBLES` / `FLOATS` (`STRONG`, `SOFT`, `WEAK`, `NULL`) | `WEAK` |
| `THREADS` | Sets `java.util.concurrent.ForkJoinPool.common.parallelism` | `64` |

```bash
java -DDEBUG_LIFECYCLE=true -DWATCH_CREATE=true -jar myapp.jar
```

When an object is reclaimed by GC without being freed (and is not `detach()`ed), `ReferenceCountingBase.finalize()` logs a debug message containing its full `referenceReport(...)` via SLF4J. See `demo/src/test/java/com/simiacryptus/demo/refcount/Test.java` for an example of turning leaked-object log output into a unit test failure.

## Best Practices

1. **Match every `addRef()` with a `freeRef()`** — Use `try`/`finally` blocks so references are released even when exceptions are thrown.
2. **Guard resource access with `assertAlive()`** — Detect use-after-free bugs immediately instead of corrupting state or dereferencing null buffers.
3. **Always call `super._free()`** — Release owned fields in `_free()` and chain to `super._free()`.
4. **Use `@RefIgnore` on `equals()` / `hashCode()`** — Prevents the autocoder from instrumenting non-owning value comparisons (see `demo`'s `BasicType`).
5. **Run tests with `-DDEBUG_LIFECYCLE=true`** — Catch leaks early with full allocation and reference-transfer stack traces.
6. **Prefer the autocoder for large codebases** — Use `refcountInsert` and `refcountVerify` to keep reference counting consistent and leak-free.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.

## Author

Copyright (c) 2020 by Andrew Charneski
