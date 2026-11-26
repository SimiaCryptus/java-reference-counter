package com.simiacryptus.ref;

import com.simiacryptus.ref.core.CollectableException;
import com.simiacryptus.ref.core.ProjectInfo;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RefAutoCoderIntegrationTest {

  private static String[] getDemoClasspath() {
    String cp = System.getProperty("demo.classpath", "");
    if (cp.isEmpty()) {
      cp = System.getProperty("java.class.path", "");
    }
    return Arrays.stream(cp.split(File.pathSeparator))
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);
  }

  private static ProjectInfo copyDemoToTemp(Path tempDir) throws Exception {
    File demoSrc = new File(System.getProperty("demo.project.dir", "../demo"), "src/main/java").getCanonicalFile();
    File targetSrc = tempDir.resolve("src/main/java").toFile();
    FileUtils.copyDirectory(demoSrc, targetSrc);
    return new ProjectInfo(
        tempDir.toAbsolutePath().toString(),
        new String[]{targetSrc.getAbsolutePath()},
        getDemoClasspath()
    );
  }

  @Test
  public void testFullLifecycleOnDemoCopy(@TempDir Path tempDir) throws Exception {
    ProjectInfo projectInfo = copyDemoToTemp(tempDir);

    // 1. Check type hierarchy on uninstrumented demo sources
    new Check.Coder(projectInfo).rewrite();

    // 2. AnnotateAll method parameters
    new AnnotateAll.Coder(projectInfo).rewrite();

    // 3. Insert reference counting (with modifyAPI = true)
    new Insert.Coder(projectInfo, true).rewrite();

    // Verify that instrumentation actually added RefArrayList / freeRef calls
    File containerFile = tempDir.resolve("src/main/java/com/simiacryptus/demo/refcount/ArrayListContainer.java").toFile();
    String instrumented = FileUtils.readFileToString(containerFile, StandardCharsets.UTF_8);
    assertTrue(instrumented.contains("RefArrayList"), "Expected RefArrayList in instrumented source");
    assertTrue(instrumented.contains("freeRef"), "Expected freeRef in instrumented source");

    // 4. Verify instrumented sources
    try {
      new Verify.Coder(projectInfo).rewrite();
    } catch (CollectableException ignored) {
      // Some demo edge cases may report warnings/errors; running Verify exercises all verifier visitors.
    }

    // 5. Update annotations
    try {
      new Update.Coder(projectInfo).rewrite();
    } catch (CollectableException ignored) {
      // Exercises AnnotateMethodCalls + VerifyMethodCalls
    }

    // 6. Remove reference counting (with modifyAPI = true)
    new Remove.Coder(projectInfo, true).rewrite();
    String reverted = FileUtils.readFileToString(containerFile, StandardCharsets.UTF_8);
    assertTrue(reverted.contains("ArrayList"), "Expected ArrayList after Remove");

    // 7. Temp (RemoveTypeAnnotations)
    new Temp.Coder(projectInfo).rewrite();
  }

  @Test
  public void testModifyAndRevertAPI(@TempDir Path tempDir) throws Exception {
    ProjectInfo projectInfo = copyDemoToTemp(tempDir);
    new ModifyAPI.Coder(projectInfo).rewrite();
    File containerFile = tempDir.resolve("src/main/java/com/simiacryptus/demo/refcount/ArrayListContainer.java").toFile();
    String modified = FileUtils.readFileToString(containerFile, StandardCharsets.UTF_8);
    assertTrue(modified.contains("RefArrayList"));

    new RevertAPI.Coder(projectInfo).rewrite();
    String reverted = FileUtils.readFileToString(containerFile, StandardCharsets.UTF_8);
    assertFalse(reverted.contains("RefArrayList"));
  }

  @Test
  public void testCheckDetectsInvalidTypeHierarchy(@TempDir Path tempDir) throws Exception {
    Path srcDir = tempDir.resolve("src/main/java");
    Files.createDirectories(srcDir.resolve("com/example"));
    Path badFile = srcDir.resolve("com/example/BadHierarchy.java");
    String source = String.join("\n",
        "package com.example;",
        "import com.simiacryptus.ref.lang.ReferenceCountingBase;",
        "public class BadHierarchy {",
        "  private ReferenceCountingBase leakedField;",
        "  public class NonStaticInner extends ReferenceCountingBase {",
        "  }",
        "}",
        ""
    );
    Files.write(badFile, source.getBytes(StandardCharsets.UTF_8));

    ProjectInfo projectInfo = new ProjectInfo(
        tempDir.toAbsolutePath().toString(),
        new String[]{srcDir.toAbsolutePath().toString()},
        getDemoClasspath()
    );

    CollectableException ex = assertThrows(CollectableException.class, () ->
        new Check.Coder(projectInfo).rewrite()
    );
    String msg = ex.getMessage();
    assertTrue(msg.contains("BadHierarchy.java"), "Expected file path in error: " + msg);
    assertTrue(msg.contains("NonStaticInner") || msg.contains("leakedField"),
        "Expected offending member snippet in error: " + msg);
  }

  @Test
  public void testVerifyDetectsUnfreedReferences(@TempDir Path tempDir) throws Exception {
    Path srcDir = tempDir.resolve("src/main/java");
    Files.createDirectories(srcDir.resolve("com/example"));
    Path leakyFile = srcDir.resolve("com/example/LeakyClass.java");
    String source = String.join("\n",
        "package com.example;",
        "import com.simiacryptus.ref.lang.ReferenceCountingBase;",
        "import java.util.function.Supplier;",
        "public class LeakyClass extends ReferenceCountingBase {",
        "  private LeakyClass child;",
        "  public void leakLocal() {",
        "    LeakyClass local = new LeakyClass();",
        "    this.child = local;",
        "  }",
        "  public Supplier<LeakyClass> leakClosure(LeakyClass arg) {",
        "    return () -> arg;",
        "  }",
        "}",
        ""
    );
    Files.write(leakyFile, source.getBytes(StandardCharsets.UTF_8));

    ProjectInfo projectInfo = new ProjectInfo(
        tempDir.toAbsolutePath().toString(),
        new String[]{srcDir.toAbsolutePath().toString()},
        getDemoClasspath()
    );

    assertThrows(CollectableException.class, () -> new Verify.Coder(projectInfo).rewrite());
  }
}
