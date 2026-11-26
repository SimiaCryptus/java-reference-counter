# refcount-autocoder

Gradle plugin and standalone AST rewriter that automatically instruments, optimizes, verifies, and removes Java reference counting code.

## Overview

`:refcount-autocoder` builds on [`:autocoder-core`](../autocoder/README.md) (Eclipse JDT AST with full type-binding resolution) and [`:refcount-core`](../core/README.md) to automate reference counting across Java projects.

- **Gradle Plugin ID:** `com.simiacryptus.refcount-autocoder`
- **Plugin Class:** `com.simiacryptus.ref.RefCountAutocoderPlugin`
- **Task Group:** `refcount`

## Gradle Plugin Configuration

```kotlin
plugins {
    java
    id("com.simiacryptus.refcount-autocoder") version "2.1.0"
}

refcountAutocoder {
    modifyAPI.set(false)    // Rewrite standard Java collection types to Ref* wrappers during insert/remove
    parallel.set(false)     // Process compilation units in parallel
    logException.set(true)  // Print CollectableException and runtime errors to stderr
    force.set(false)        // Execute even if refcount-core is not found in project dependencies
}
```

All four extension properties also fall back to JVM system properties of the same name (`-DmodifyAPI=true`, `-Dparallel=true`, `-DlogException=true`, `-Dforce=true`).

> **Note:** Every `RefAutoCoderTask` checks whether `refcount-core` is present in the project's dependencies (or `ReferenceCounting.java` exists in the project sources). If not found, the task logs a warning and skips execution unless `force` is set to `true`.

## Gradle Tasks

| Task | Task Class | Description |
|---|---|---|
| `refcountInsert` | `Insert` | Full instrumentation pipeline: validates hierarchy, strips old refs, optionally rewrites collection APIs, inserts `addRef()`/`freeRef()`/`_free()`/closure handling, and optimizes redundant ref ops |
| `refcountRemove` | `Remove` | Strips inserted `addRef()`/`freeRef()` calls, inlines temporary ref variables, and optionally reverts `Ref*` types (`modifyAPI=true`) |
| `refcountVerify` | `Verify` | Runs all static verification passes without modifying files and aggregates errors into a `CollectableException` |
| `refcountCheck` | `Check` | Runs `ValidateTypeHierarchy` to ensure reference-counted types obey inheritance rules |
| `refcountUpdate` | `Update` | Scans method calls for missing `@RefAware` annotations, inserts them, and runs `Verify` |
| `refcountModifyAPI` | `ModifyAPI` | Converts standard Java collection/stream/functional types to `com.simiacryptus.ref.wrappers.Ref*` types and inserts lifecycle methods |
| `refcountRevertAPI` | `RevertAPI` | Reverts `Ref*` wrapper types back to standard Java types and removes generated ref methods |
| `refcountAnnotate` | `AnnotateAll` | Annotates reference-counted method parameters with `@RefAware` |
| `refcountTemp` | `Temp` | Removes `@RefAware` type annotations from source files |
| `refcountPrintAST` | `PrintAST` | Prints the Eclipse JDT AST of project Java files to aid debugging |

## Transformation Pipelines

### `Insert.Coder`

1. `Check.Coder` (`ValidateTypeHierarchy`)
2. `Remove.Coder` (clears existing instrumentation without reverting API types)
3. If `modifyAPI == true`: `ModifyAPI.Coder`; otherwise: `RemoveRefMethods` + `InsertMethods` (generates `_free()` and covariant `addRef()` overrides)
4. `InsertAddRefs` (array initializers, object creation, constructor calls, method calls, return statements, assignments, variable declarations)
5. `ModifyAssignments` (safely frees overwritten references before reassigning)
6. `InsertFreeRefs` (local variables, parameters, object creations, method invocations)
7. `InstrumentClosures` (anonymous classes and lambdas capturing reference-counted variables)
8. `OptimizeRefs` (eliminates redundant `addRef`/`freeRef` pairs)

### `Verify.Coder`

Runs in fail-at-end mode and collects all diagnostics across:
1. `IndexSymbols` (builds a project-wide `SymbolIndex`)
2. `VerifyMethodCalls`
3. `VerifyAssignments`
4. `VerifyFields`
5. `VerifyClosures`
6. `VerifyMethodVariables`

## Controlling Instrumentation with Annotations

- **`@RefIgnore`**: Place on a class, method, or parameter to opt out of instrumentation and verification (commonly used on `equals()`, `hashCode()`, and low-level utility methods).
- **`@RefAware`**: Indicates that a parameter, return value, or type participates in reference ownership transfer.
- **`@MustCall`**: Marks methods (such as `_free()`) that subclasses must invoke via `super`.

## Standalone CLI / Build-Time Instrumentation

`com.simiacryptus.ref.Insert` provides a `main` entry point for running instrumentation directly via `JavaExec` (for example, to instrument sources into a build directory without modifying tracked source files):

```
java -cp <autocoder-classpath> com.simiacryptus.ref.Insert \
    <projectRoot> <sourceDirs> <classpath> [modifyAPI] [annotate]
```

- `<sourceDirs>` and `<classpath>` use the platform `File.pathSeparator` (`:` on Linux/macOS, `;` on Windows).
- `[modifyAPI]` (`true`/`false`, default `false`): rewrite standard Java collection types to `Ref*` wrappers.
- `[annotate]` (`true`/`false`, default `false`): run `AnnotateAll.Coder` before `Insert.Coder`.

See [`demo/build.gradle.kts`](../demo/build.gradle.kts) for a complete working example.
