# autocoder-core

Generic Eclipse JDT–based Java AST parsing, analysis, and source-rewriting framework for Gradle tasks.

## Overview

`:autocoder-core` provides the build-tool and AST-rewriting foundation used by [`:refcount-autocoder`](../refcount-autocoder/README.md). It parses Java compilation units with Eclipse JDT (`org.eclipse.jdt.core`) using full classpath and sourcepath binding resolution, applies structured `ASTVisitor` / `ASTRewrite` passes, and writes modified files back to disk.

## Key Classes

### `com.simiacryptus.ref.core`

| Class | Description |
|---|---|
| `AutoCoder` | Abstract orchestrator that parses project files via `ProjectInfo` and runs `ASTEditor` passes sequentially or in parallel (`rewrite(...)` and read-only `scan(...)`) |
| `AutoCoderTask` | Abstract Gradle `DefaultTask` that discovers Java `SourceSet` directories and `compileClasspath` / `testCompileClasspath` dependencies to construct a `ProjectInfo` |
| `ProjectInfo` | Configures Eclipse JDT `ASTParser` (JLS11 with binding, statement, and binding recovery enabled) and parses Java source files into `CompilationUnit` maps |
| `SymbolIndex` | Cross-file symbol registry populated by `IndexSymbols` for whole-project analysis |
| `CollectableException` | Aggregates multiple AST verification errors across files into a single exception report |
| `ASTUtil` | Helper utilities for inspecting and manipulating Eclipse JDT AST nodes and bindings |
| `ReflectionUtil` | Reflection helpers used alongside JDT type bindings |
| `PrintAST` | Gradle task (`AutoCoderTask`) that dumps the Eclipse JDT AST structure of project source files |

### `com.simiacryptus.ref.core.ops`

| Class | Description |
|---|---|
| `ASTEditor` | Base visitor supporting tracked AST modifications (`ASTRewrite`) and writing or reverting changes |
| `ASTOperator` | Higher-level AST transformation base class extending `ASTEditor` |
| `ASTScanner` | Read-only AST visitor for analysis and verification passes |
| `IndexSymbols` | Visitor that populates `SymbolIndex` across compilation units |
| `LoggingASTVisitor` | AST visitor with formatted error/diagnostic reporting including source snippets and file/line locations |
| `LogNodes` | Diagnostic visitor that logs visited AST nodes |

## Extending `AutoCoder`

To implement a custom AST transformation pass:

1. Subclass `ASTEditor` (or `ASTOperator` / `ASTScanner`) and override the desired Eclipse JDT `visit(...)` methods.
2. Subclass `AutoCoder` and invoke `rewrite(MyVisitor::new)` inside `rewrite()`.
3. Optionally subclass `AutoCoderTask` to expose your `AutoCoder` as a Gradle task.
