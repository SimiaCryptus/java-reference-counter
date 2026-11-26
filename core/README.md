# refcount-core

Core runtime library for deterministic reference counting in Java.

## Overview

`:refcount-core` provides the runtime primitives, diagnostic tools, object pooling utilities, and reference-aware standard library wrappers used by reference-counted Java applications and by code instrumented with [`:refcount-autocoder`](../refcount-autocoder/README.md).

## Dependency

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("com.simiacryptus:refcount-core:2.1.0")
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

## Package Reference

### `com.simiacryptus.ref.lang`

Core reference counting contracts, base implementations, annotations, and pooling:

| Type | Description |
|---|---|
| `ReferenceCounting` | Interface defining `addRef()`, `freeRef()`, `isFreed()`, `currentRefCount()`, `assertAlive()`, `tryAddRef()`, and `detach()` (with no-op defaults for non-counted implementations) |
| `ReferenceCountingBase` | Thread-safe abstract base class backed by `AtomicInteger` and `AtomicBoolean`, with lifecycle stack-trace tracking and GC leak reporting |
| `RefUtil` | Null-safe static helpers (`addRef`, `freeRef`, `addRefs`, `freeRefs`, `wrapInterface`, `assertAlive`, `watch`, `set`, `isNotNull`, `Optional` helpers) |
| `RefAware` | Runtime annotation marking parameters, return values, or types that participate in reference ownership transfer |
| `RefIgnore` | Runtime annotation excluding classes, methods, or parameters from autocoder processing |
| `MustCall` | Runtime annotation marking methods (such as `_free()`) that subclasses must call via `super` |
| `LifecycleException` | `RuntimeException` thrown on use-after-free, double-free, or `assertFreed()` failures, embedding the object's reference report |
| `RecycleBin<T>` | Size-bucketed object pool with built-in `DOUBLES` and `FLOATS` instances, background age-based purging, and `StackCounter` profiling |
| `PersistanceMode` | Enum (`STRONG`, `SOFT`, `WEAK`, `NULL`) controlling how `RecycleBin` retains pooled buffers |
| `RefLazyVal<T>` | Thread-safe, reference-counted lazy `Supplier<T>` that increments the reference on each `get()` and frees the cached value on `_free()` |
| `ReferenceWrapper<T>` | Lightweight wrapper pairing any object with an idempotent `Consumer<T>` destructor (`peek()`, `unwrap()`, `destroy()`) |

### `com.simiacryptus.ref.wrappers`

Reference-counted wrappers for Java standard library types. These wrappers automatically manage element reference counts on insertion, retrieval, iteration, and disposal:

| Category | Classes |
|---|---|
| **Lists** | `RefList`, `RefAbstractList`, `RefArrayList`, `RefLinkedList`, `RefLists` |
| **Sets** | `RefSet`, `RefAbstractSet`, `RefHashSet`, `RefTreeSet`, `RefSortedSet`, `RefNavigableSet` |
| **Maps** | `RefMap`, `RefAbstractMap`, `RefHashMap`, `RefLinkedHashMap`, `RefTreeMap`, `RefConcurrentHashMap`, `RefSortedMap`, `RefNavigableMap`, `RefMaps`, `RefEntry` |
| **Collections & Queues** | `RefCollection`, `RefAbstractCollection`, `RefCollections`, `RefQueue`, `RefAbstractQueue`, `RefDeque`, `RefBlockingQueue`, `RefLinkedBlockingQueue`, `RefConcurrentLinkedDeque` |
| **Streams** | `RefStream`, `RefIntStream`, `RefDoubleStream`, `RefLongStream`, `StreamWrapper`, `RefCollectors`, `RefStreamSupport` |
| **Iterators & Spliterators** | `RefIterator`, `RefIteratorBase`, `RefListIterator`, `RefPrimitiveIterator`, `RefSpliterator`, `RefSpliterators` |
| **Functional Interfaces** | `RefFunction`, `RefBiFunction`, `RefConsumer`, `RefPredicate`, `RefSupplier`, `RefComparator` |
| **Concurrency & Utilities** | `RefAtomicReference`, `RefThreadLocal`, `RefWeakReference`, `RefArrays`, `RefAssert`, `RefString`, `RefStringBuilder`, `RefSystem` |

### `com.simiacryptus.ref`

| Type | Description |
|---|---|
| `RefSettings` | Singleton configuration loaded from JVM system properties, plus mutable stack-trace formatting settings |

### `com.simiacryptus.lang`

| Type | Description |
|---|---|
| `Settings` | System-property reader (`Settings.get(...)`) and JSON serialization helper |
| `StackCounter` | Aggregates counts/totals keyed by call stack for allocation and recycle-bin profiling |
| `DoubleStatistics` | Running summary statistics for `double` values |
| `LazyVal<T>` | Non-reference-counted lazy value holder |
| `Tuple2<A, B>` | Simple immutable pair |

## Lifecycle Semantics

Understanding the exact behavior of `ReferenceCountingBase` avoids subtle bugs:

- **Initial state:** A newly constructed `ReferenceCountingBase` starts with `currentRefCount() == 1` and `isFreed() == false`.
- **`addRef()`:** Atomically increments the count if greater than `0`. Calling `addRef()` on an already-freed object (`count == 0`) throws `IllegalStateException` containing `referenceReport(...)`.
- **`tryAddRef()`:** Atomically increments the count if alive and returns `true`; returns `false` without throwing if the object is already freed.
- **`freeRef()`:**
  - If `isFreed()` is already `true`, returns `0` immediately.
  - Atomically decrements the count. If the count drops below `0` (and the object is not detached and not inside the JVM finalizer), throws `LifecycleException`.
  - When the count reaches `0` (and the object is not detached), atomically marks `isFreed = true` and invokes `_free()` exactly once.
- **`detach()`:** Marks the object as detached so `freeRef()` will not trigger `_free()` when the count reaches `0`, and GC finalization will not log a leak warning.
- **`assertAlive()` / `assertFreed()`:** Throws `LifecycleException` if the object is not in the expected state.
- **GC fallback:** If an un-freed, non-detached object is garbage-collected, `finalize()` logs its `referenceReport(...)` at `DEBUG` level (unless `ReferenceCountingBase.supressLog` is `true`) and invokes `_free()`.

## Usage Examples

### Implementing a Reference-Counted Class

```java
import com.simiacryptus.ref.lang.MustCall;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

public class BufferResource extends ReferenceCountingBase {
    private double[] buffer;

    public BufferResource(int length) {
        this.buffer = RecycleBin.DOUBLES.obtain(length);
    }

    public double[] getBuffer() {
        assertAlive();
        return buffer;
    }

    @Override
    public BufferResource addRef() {
        return (BufferResource) super.addRef();
    }

    @MustCall
    @Override
    protected void _free() {
        if (buffer != null) {
            RecycleBin.DOUBLES.recycle(buffer, buffer.length);
            buffer = null;
        }
        super._free();
    }
}
```

### Working with `RefUtil`

`RefUtil` safely handles `null`, `ReferenceCounting` instances, object arrays (recursively), `Map.Entry`, and `Optional`:

```java
import com.simiacryptus.ref.lang.RefUtil;

// Safe addRef / freeRef on generic or nullable references
T ref = RefUtil.addRef(maybeRefCounted);
try {
    RefUtil.assertAlive(ref);
} finally {
    RefUtil.freeRef(ref);
}

// Replace an array element while freeing the previous element
RefUtil.set(resourceArray, 0, newResource);

// Wrap a plain interface implementation so it carries ReferenceCounting ownership of captured refs
Runnable wrapped = RefUtil.wrapInterface(() -> doWork(resource), resource);
```

### Using `RecycleBin`

`RecycleBin` pools sized buffers (such as `double[]` and `float[]`) to reduce allocation churn:

```java
import com.simiacryptus.ref.lang.PersistanceMode;
import com.simiacryptus.ref.lang.RecycleBin;

double[] buf = RecycleBin.DOUBLES.obtain(1024);
try {
    // use zero-initialized buffer
} finally {
    RecycleBin.DOUBLES.recycle(buf, buf.length);
}

// Optional tuning
RecycleBin.DOUBLES
    .setPersistanceMode(PersistanceMode.SOFT)
    .setMaxItemsPerBuffer(100)
    .setPurgeFreq(10); // seconds
```

## Configuration

`RefSettings` reads all settings from **JVM system properties** (`System.getProperty`):

| System Property | Description | Default |
|---|---|---|
| `DEBUG_LIFECYCLE` | Capture creation, `addRef()`, and `freeRef()` traces for all `ReferenceCountingBase` instances | `false` |
| `WATCH_ENABLE` | Enable per-instance `watch()` trace recording | `true` |
| `WATCH_CREATE` | Capture creation stack traces even when `DEBUG_LIFECYCLE` is `false` | `false` |
| `DOUBLE_CACHE_MODE` | `PersistanceMode` for `RecycleBin.DOUBLES` and `RecycleBin.FLOATS` (`STRONG`, `SOFT`, `WEAK`, `NULL`) | `WEAK` |
| `THREADS` | Parallelism for `java.util.concurrent.ForkJoinPool.common.parallelism` | `64` |

```bash
java -DDEBUG_LIFECYCLE=true -DWATCH_CREATE=true -DDOUBLE_CACHE_MODE=SOFT -jar myapp.jar
```

Runtime static fields on `RefSettings`:

```java
import com.simiacryptus.ref.RefSettings;

RefSettings.maxStackSize = 50;                      // Max stack frames captured per trace (default: 50)
RefSettings.stackPrefixFilter = "com.simiacryptus"; // Prefix used by RefSettings.filter(...)
// RefSettings.maxTracesPerObject is final (100)
```

## Thread Safety

- Reference counts are updated atomically with `AtomicInteger`.
- Single-execution disposal is guaranteed with `AtomicBoolean.getAndSet(true)`.
- Trace lists (`addRef`, `freeRefs`) synchronize on each list when recording.
- `RefUtil.addRef` / `RefUtil.freeRef` / `RefUtil.set` synchronize on array instances when traversing or updating elements.
- `RecycleBin` buckets use `ConcurrentHashMap` and `ConcurrentLinkedDeque`.
