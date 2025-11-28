# refcount-core

Reference Counting Runtime Core for Java

## Overview

This module provides the core runtime library for reference counting in Java. It includes:

- **Base Classes**: `ReferenceCountingBase` for creating reference-counted objects
- **Interfaces**: `ReferenceCounting` interface defining the reference counting contract
- **Utilities**: `RefUtil` for working with reference-counted objects
- **Annotations**: `@RefAware`, `@RefIgnore`, `@MustCall` for code documentation and autocoder hints
- **Collection Wrappers**: Reference-aware implementations of Java collections

## Package Structure

### `com.simiacryptus.ref.lang`

Core reference counting infrastructure:

| Class | Description |
|-------|-------------|
| `ReferenceCounting` | Core interface for reference-counted objects |
| `ReferenceCountingBase` | Abstract base class implementing reference counting |
| `RefUtil` | Static utility methods for reference management |
| `RefAware` | Annotation marking reference-aware parameters |
| `RefIgnore` | Annotation to exclude code from autocoder processing |
| `MustCall` | Annotation indicating methods that must be called |
| `LifecycleException` | Exception thrown on lifecycle violations |
| `RecycleBin` | Object pooling support |

### `com.simiacryptus.ref.wrappers`

Reference-counted collection wrappers:

| Category | Classes |
|----------|---------|
| **Lists** | `RefArrayList`, `RefLinkedList`, `RefAbstractList` |
| **Sets** | `RefHashSet`, `RefTreeSet`, `RefSortedSet`, `RefNavigableSet` |
| **Maps** | `RefHashMap`, `RefLinkedHashMap`, `RefTreeMap`, `RefConcurrentHashMap` |
| **Queues** | `RefLinkedBlockingQueue`, `RefConcurrentLinkedDeque` |
| **Streams** | `RefStream`, `RefIntStream`, `RefDoubleStream`, `RefLongStream` |
| **Iterators** | `RefIterator`, `RefListIterator`, `RefSpliterator` |
| **Functional** | `RefFunction`, `RefConsumer`, `RefPredicate`, `RefSupplier` |

### `com.simiacryptus.lang`

General utilities:

| Class | Description |
|-------|-------------|
| `Settings` | Configuration management |
| `StackCounter` | Stack tracking utilities |
| `Tuple2` | Simple tuple implementation |
| `LazyVal` | Lazy value initialization |

## Usage

### Maven Dependency

```xml
<dependency>
    <groupId>com.simiacryptus</groupId>
    <artifactId>refcount-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Creating a Reference-Counted Class

```java
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.lang.MustCall;

public class MyResource extends ReferenceCountingBase {
    private Object resource;
    
    public MyResource() {
        this.resource = allocateResource();
    }
    
    public void useResource() {
        assertAlive();
        // ... use resource
    }
    
    @MustCall
    @Override
    protected void _free() {
        resource = null; // Release resource
        super._free();
    }
}
```

### Using Reference-Counted Collections

```java
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.lang.RefUtil;

RefArrayList<MyResource> list = new RefArrayList<>();
try {
    list.add(new MyResource());
    list.add(new MyResource());
    
    // Get returns a new reference
    MyResource item = list.get(0);
    try {
        item.useResource();
    } finally {
        RefUtil.freeRef(item);
    }
} finally {
    list.freeRef(); // Frees list and contained elements
}
```

## Configuration

Reference counting behavior can be configured through `RefSettings`:

```java
RefSettings settings = RefSettings.INSTANCE();

// Enable debugging for specific classes
settings.setLifecycleDebug(MyResource.class, true);

// Configure stack trace depth
RefSettings.maxStackSize = 20;
RefSettings.maxTracesPerObject = 10;
```

## Thread Safety

The reference counting implementation is thread-safe:
- Reference count is managed with `AtomicInteger`
- Freed state is managed with `AtomicBoolean`
- Collection wrappers synchronize appropriately
