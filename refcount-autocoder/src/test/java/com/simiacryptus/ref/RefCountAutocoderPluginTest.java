package com.simiacryptus.ref;

import com.simiacryptus.ref.core.PrintAST;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RefCountAutocoderPluginTest {

  @Test
  public void testPluginRegistersTasksAndExecutesCheck(@TempDir Path tempDir) throws Exception {
    Path srcDir = tempDir.resolve("src/main/java/com/example");
    Files.createDirectories(srcDir);
    Files.write(
        srcDir.resolve("Clean.java"),
        "package com.example;\npublic class Clean { public int x() { return 1; } }\n"
            .getBytes(StandardCharsets.UTF_8)
    );

    Project project = ProjectBuilder.builder()
        .withProjectDir(tempDir.toFile())
        .build();
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("com.simiacryptus.refcount-autocoder");

    RefCountAutocoderExtension ext = project.getExtensions().findByType(RefCountAutocoderExtension.class);
    assertNotNull(ext);
    assertFalse(ext.getParallel().get());
    assertTrue(ext.getLogException().get());
    assertFalse(ext.getForce().get());
    assertFalse(ext.getModifyAPI().get());

    List<String> expectedTasks = Arrays.asList(
        "refcountInsert",
        "refcountRemove",
        "refcountVerify",
        "refcountCheck",
        "refcountUpdate",
        "refcountModifyAPI",
        "refcountRevertAPI",
        "refcountAnnotate",
        "refcountTemp",
        "refcountPrintAST"
    );
    for (String taskName : expectedTasks) {
      Task task = project.getTasks().findByName(taskName);
      assertNotNull(task, "Missing task: " + taskName);
      assertEquals(RefCountAutocoderPlugin.GROUP, task.getGroup());
    }

    // Execute refcountCheck and refcountPrintAST actions directly
    Check checkTask = (Check) project.getTasks().getByName("refcountCheck");
    checkTask.executeTask();

    PrintAST printTask = (PrintAST) project.getTasks().getByName("refcountPrintAST");
    printTask.executeTask();
  }
}
