package com.simiacryptus.ref.bytecode;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RefCountBytecodePluginTest {

  @Test
  public void testPluginRegistersTasksAndVerifiesMultiLanguageOutputs(@TempDir Path tempDir) throws Exception {
    // Set up simulated compiled Java and Kotlin output directories in a Gradle project
    Path javaClassesDir = tempDir.resolve("build/classes/java/main");
    Path kotlinClassesDir = tempDir.resolve("build/classes/kotlin/main");
    Files.createDirectories(javaClassesDir);
    Files.createDirectories(kotlinClassesDir);

    byte[] kRes = NonJavaBytecodeBuilder.buildKotlinResourceClass("pkg/kt/KRes");
    byte[] validKt = NonJavaBytecodeBuilder.buildValidKotlinContainer("pkg/kt/KotlinContainer", "pkg/kt/KRes");
    InMemoryJavaCompiler.writeClassesToDir(Map.of(
        "pkg.kt.KRes", kRes,
        "pkg.kt.KotlinContainer", validKt
    ), kotlinClassesDir);

    Map<String, byte[]> javaClean = InMemoryJavaCompiler.compileSingle(
        "com.example.PlainHelper",
        "package com.example;\npublic class PlainHelper { public static int add(int a, int b) { return a + b; } }\n"
    );
    InMemoryJavaCompiler.writeClassesToDir(javaClean, javaClassesDir);

    Project project = ProjectBuilder.builder()
        .withProjectDir(tempDir.toFile())
        .build();
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("com.simiacryptus.refcount-bytecode");

    // Register Kotlin classes output directory alongside Java in main SourceSet (mirrors org.jetbrains.kotlin.jvm)
    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    SourceSet main = sourceSets.getByName("main");
    main.getOutput().dir(kotlinClassesDir.toFile());

    RefCountBytecodeExtension ext = project.getExtensions().findByType(RefCountBytecodeExtension.class);
    assertNotNull(ext);
    assertTrue(ext.getIncludeTests().get());
    assertTrue(ext.getFailOnError().get());
    assertFalse(ext.getForce().get());

    // Enable force=true since ProjectBuilder project doesn't resolve external Maven dependencies
    ext.getForce().set(true);

    for (String taskName : new String[]{"refcountBytecodeVerify", "refcountBytecodeCheck", "refcountBytecodeReport"}) {
      Task task = project.getTasks().findByName(taskName);
      assertNotNull(task, "Expected task: " + taskName);
      assertEquals(RefCountBytecodePlugin.GROUP, task.getGroup());
    }

    // 1. Clean Java + Kotlin classes pass both check and verify tasks
    BytecodeCheckTask checkTask = (BytecodeCheckTask) project.getTasks().getByName("refcountBytecodeCheck");
    checkTask.executeTask();

    BytecodeVerifyTask verifyTask = (BytecodeVerifyTask) project.getTasks().getByName("refcountBytecodeVerify");
    verifyTask.executeTask();

    BytecodeReportTask reportTask = (BytecodeReportTask) project.getTasks().getByName("refcountBytecodeReport");
    reportTask.executeTask();
    assertTrue(reportTask.getTextReport().get().getAsFile().exists());
    assertTrue(reportTask.getJsonReport().get().getAsFile().exists());

    // 2. Add a leaky Kotlin class -> refcountBytecodeVerify fails with BytecodeVerificationException
    byte[] buggyKt = NonJavaBytecodeBuilder.buildBuggyKotlinContainer("pkg/kt/BuggyKotlin", "pkg/kt/KRes");
    InMemoryJavaCompiler.writeClassesToDir(Collections.singletonMap("pkg.kt.BuggyKotlin", buggyKt), kotlinClassesDir);

    assertThrows(BytecodeVerificationException.class, verifyTask::executeTask);

    // Report task still succeeds by default (failOnError = false) and records diagnostics
    reportTask.executeTask();
    String json = Files.readString(reportTask.getJsonReport().get().getAsFile().toPath());
    assertTrue(json.contains("BuggyKotlin"));
  }
}
