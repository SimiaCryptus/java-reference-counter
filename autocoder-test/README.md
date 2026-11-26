# autocoder-test

Test harness utilities for running [`:autocoder-core`](../autocoder/README.md) and [`:refcount-autocoder`](../refcount-autocoder/README.md) passes against Gradle projects.

## Overview

`:autocoder-test` provides `SimpleGradleProject` (`com.simiacryptus.ref.SimpleGradleProject`), a lightweight helper that loads a Gradle project's `src/main/java` and `src/test/java` directories together with a resolved classpath and produces a `ProjectInfo` instance ready for `AutoCoder` execution.

## Usage in Tests

```java
SimpleGradleProject project = SimpleGradleProject.load(projectDir);
ProjectInfo info = project.getProjectInfo();
new Insert.Coder(info, true).rewrite();
```

`SimpleGradleProject.load(root)` resolves the classpath from the `demo.classpath` JVM system property (falling back to `java.class.path`). In this repository, `autocoder-test/build.gradle.kts` and `refcount-autocoder/build.gradle.kts` wire both `demo.project.dir` and `demo.classpath` automatically to the `:refcount-demo` project during `./gradlew test`.
