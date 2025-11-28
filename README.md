# Java Reference Counter

A toolkit for implementing Reference Counting resource management in Java. This library provides a framework for deterministic resource management through reference counting, similar to how languages like C++ and Rust handle memory management.

## Overview

Java's garbage collector provides automatic memory management, but it doesn't offer deterministic resource cleanup. This can be problematic when working with:
- Native resources (e.g., GPU memory, file handles)
- Large objects that need timely cleanup
- Resources that require ordered cleanup sequences

The Java Reference Counter toolkit solves this by providing:
1. **A reference counting base class** for creating reference-counted objects
2. **Reference-aware collection wrappers** that properly manage element lifecycles
3. **A Maven plugin (autocoder)** that automatically instruments your code with reference counting operations

## Modules

| Module | Artifact ID | Description |
|--------|-------------|-------------|
| **core** | `refcount-core` | Core runtime library with base classes, interfaces, and reference-counted collection wrappers |
| **autocoder** | `autocoder-core` | Base Maven plugin infrastructure for AST transformation |
| **refcount-autocoder** | `refcount-autocoder` | Maven plugin that automatically instruments code with reference counting operations |
| **autocoder-test** | `autocoder-test` | Test utilities for the autocoder |
| **demo** | `demo` | Example implementations demonstrating usage patterns |

## Getting Started

### Maven Dependency

Add the core library to your project:

```xml
<dependency>
    <groupId>com.simiacryptus</groupId>
    <artifactId>refcount-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Creating Reference-Counted Objects

Extend `ReferenceCountingBase` to create objects with automatic reference counting:

```java
import com.simiacryptus.ref.lang.ReferenceCountingBase;

public class MyResource extends ReferenceCountingBase {
    private byte[] data;
    
    public MyResource(int size) {
        this.data = new byte[size];
    }
    
    public void doWork() {
        assertAlive(); // Throws if object has been freed
        // ... use the resource
    }
    
    @Override
    protected void _free() {
        // Clean up resources when reference count reaches zero
        data = null;
        super._free();
    }
}
```

### Basic Usage Pattern

```java
// Create a reference-counted object (starts with refcount = 1)
MyResource resource = new MyResource(1024);

// Pass to another owner - increment reference count
MyResource ref2 = resource.addRef();

// When done with a reference, decrement count
resource.freeRef();

// Object is freed when last reference is released
ref2.freeRef(); // _free() is called here
```

### Using Reference-Counted Collections

The library provides reference-aware wrappers for standard Java collections:

```java
import com.simiacryptus.ref.wrappers.*;

// Reference-counted ArrayList
RefArrayList<MyResource> list = new RefArrayList<>();
list.add(new MyResource(100));

// Elements are properly reference-counted
MyResource item = list.get(0); // Returns a new reference
item.freeRef(); // Release when done

// Free the list (also frees contained elements)
list.freeRef();
```

Available collection wrappers include:
- `RefArrayList`, `RefLinkedList`
- `RefHashMap`, `RefLinkedHashMap`, `RefTreeMap`, `RefConcurrentHashMap`
- `RefHashSet`, `RefTreeSet`
- `RefStream`, `RefDoubleStream`, `RefIntStream`, `RefLongStream`
- `RefIterator`, `RefSpliterator`
- And more...

## Key Classes and Interfaces

### Core Interfaces

| Interface/Class | Description |
|----------------|-------------|
| `ReferenceCounting` | Core interface defining reference counting operations (`addRef()`, `freeRef()`, `isFreed()`, etc.) |
| `ReferenceCountingBase` | Abstract base class implementing `ReferenceCounting` with lifecycle debugging support |
| `RefUtil` | Utility methods for working with reference-counted objects |

### Annotations

| Annotation | Description |
|------------|-------------|
| `@RefAware` | Marks a parameter/return value as being reference-aware (caller owns the reference) |
| `@RefIgnore` | Marks code that should be ignored by the autocoder |
| `@MustCall` | Indicates a method that must be called (used for `_free()` override) |

### Lifecycle Methods

```java
public interface ReferenceCounting {
    ReferenceCounting addRef();     // Increment reference count
    int freeRef();                  // Decrement reference count
    boolean isFreed();              // Check if object has been freed
    int currentRefCount();          // Get current reference count
    boolean assertAlive();          // Assert object is not freed
    boolean tryAddRef();            // Try to add reference (returns false if freed)
    ReferenceCounting detach();     // Detach from reference counting
}
```

## Maven Plugin (Autocoder)

The `refcount-autocoder` Maven plugin can automatically instrument your code with reference counting operations, reducing boilerplate and preventing resource leaks.

### Plugin Configuration

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.simiacryptus</groupId>
            <artifactId>refcount-autocoder</artifactId>
            <version>2.1.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>insert</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Available Goals

| Goal | Description |
|------|-------------|
| `insert` | Insert reference counting operations into source code |
| `remove` | Remove reference counting operations from source code |
| `verify` | Verify that reference counting is correctly implemented |
| `check` | Check code for reference counting issues |

## Lifecycle Debugging

The library includes comprehensive debugging support for tracking reference leaks:

```java
// Enable lifecycle debugging for specific classes
RefSettings.INSTANCE().setLifecycleDebug(MyResource.class, true);

// Watch specific objects for debugging
resource.watch();

// Get detailed reference report
String report = ReferenceCountingBase.referenceReport(resource, true);
```

Debug output includes:
- Stack traces where objects were created
- Stack traces for each `addRef()` call
- Stack traces for each `freeRef()` call
- Current reference count and state

## Best Practices

1. **Always free references** - Every `addRef()` must have a corresponding `freeRef()`
2. **Use try-finally** - Ensure `freeRef()` is called even when exceptions occur
3. **Check `assertAlive()`** - Call in methods that use the resource
4. **Override `_free()`** - Release resources when reference count reaches zero
5. **Use the autocoder** - Let the plugin manage reference counting automatically

```java
MyResource resource = new MyResource(1024);
try {
    resource.doWork();
} finally {
    resource.freeRef();
}
```

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.

## Author

Copyright (c) 2020 by Andrew Charneski
