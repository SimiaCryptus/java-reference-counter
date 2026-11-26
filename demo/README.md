# refcount-demo

End-to-end demonstration and leak-verification project for `refcount-core` and `refcount-autocoder`.

## Overview

`:refcount-demo` contains clean, uninstrumented Java source files in `src/main/java` that use `ReferenceCountingBase` and standard Java collections. During the Gradle build, `:refcount-demo` copies those sources into `build/instrumented-src/main/java`, runs `com.simiacryptus.ref.Insert` (with `modifyAPI=true` and `annotate=true`) against the copied sources, and compiles the instrumented output.

This setup demonstrates two things:
1. **Non-destructive build-time instrumentation** — original source files remain concise and readable while compiled bytecode has full reference counting.
2. **Automated zero-leak testing** — `Test` runs `TestOperations.main()` with `-DDEBUG_LIFECYCLE=true`, forces GC, and fails the test if `ReferenceCountingBase` logs even a single unfreed object or lifecycle warning.

## Included Examples (`com.simiacryptus.demo.refcount`)

| Class | What It Demonstrates |
|---|---|
| `BasicType` | Leaf `ReferenceCountingBase` implementation with `assertAlive()`, `_free()`, `@RefIgnore` on `equals()`/`hashCode()`, and `Comparable` |
| `SimpleContainer` | Reference-counted object holding a `BasicType` field |
| `WrapperType<T>` | Generic wrapper over `T extends ReferenceCounting` |
| `ArrayContainer` | Reference-counted object managing an array of `BasicType` elements |
| `ArrayListContainer`, `LinkedListContainer` | `ArrayList` / `LinkedList` operations (and streams/iterators) automatically rewritten to `RefArrayList` / `RefLinkedList` |
| `HashSetContainer`, `TreeSetContainer` | `HashSet` / `TreeSet` operations automatically rewritten to `RefHashSet` / `RefTreeSet` |
| `HashMapValuesContainer`, `LinkedHashMapValuesContainer`, `ConcurrentHashMapValuesContainer` | `HashMap` / `LinkedHashMap` / `ConcurrentHashMap` operations automatically rewritten to `RefHashMap` / `RefLinkedHashMap` / `RefConcurrentHashMap` |
| `DeququeContainer` | `ConcurrentLinkedDeque` operations (and streams/iterators) automatically rewritten to `RefConcurrentLinkedDeque` |
| `RefAwareConsumer` | Functional interface / lambda interaction with reference-counted parameters |
| `TestOperations` | Exercises containers, streams, closures, and collections end-to-end |

## Running the Demo

```bash
# Instrument sources, compile, and run the zero-leak test suite
./gradlew :refcount-demo:test
```

After running the build, you can inspect the generated reference-counted sources in `demo/build/instrumented-src/main/java/`.
