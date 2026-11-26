package com.simiacryptus.ref.core;

import com.simiacryptus.ref.core.ops.ASTEditor;
import com.simiacryptus.ref.core.ops.IndexSymbols;
import com.simiacryptus.ref.core.ops.LoggingASTVisitor;
import org.eclipse.jdt.core.dom.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class AutoCoderCoreTest {

  private static class SampleTarget {
    private final String secret = "hello";
  }

  @Test
  public void testCollectableException() {
    CollectableException e1 = new CollectableException("error one");
    CollectableException e2 = new CollectableException("error two");
    CollectableException combined = CollectableException.combine(Arrays.asList(e1, e2));
    assertTrue(combined.getMessage().contains("error one"));
    assertTrue(combined.getMessage().contains("error two"));
  }

  @Test
  public void testReflectionUtil() {
    SampleTarget target = new SampleTarget();
    String val = ReflectionUtil.getField(target, "secret");
    assertEquals("hello", val);
    assertNull(ReflectionUtil.getField(target, "nonexistentField"));
  }

  @Test
  public void testProjectInfoParseSymbolIndexAndLoggingVisitor(@TempDir Path tempDir) throws Exception {
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir.resolve("com/example"));
    Path javaFile = srcDir.resolve("com/example/Sample.java");
    String source = String.join("\n",
        "package com.example;",
        "public class Sample {",
        "  private int count = 0;",
        "  public int add(int x) {",
        "    int y = count + x;",
        "    Runnable r = new Runnable() { public void run() { count++; } };",
        "    r.run();",
        "    return y;",
        "  }",
        "}",
        ""
    );
    Files.write(javaFile, source.getBytes(StandardCharsets.UTF_8));

    ProjectInfo projectInfo = new ProjectInfo(
        tempDir.toAbsolutePath().toString(),
        new String[]{srcDir.toAbsolutePath().toString()},
        new String[]{}
    );
    assertTrue(projectInfo.toString().contains("ProjectInfo{"));
    File[] files = projectInfo.sourceFiles();
    assertEquals(1, files.length);

    HashMap<File, CompilationUnit> parsed = projectInfo.parse();
    assertEquals(1, parsed.size());
    CompilationUnit cu = parsed.values().iterator().next();
    File file = parsed.keySet().iterator().next();

    // Test IndexSymbols and SymbolIndex
    SymbolIndex index = new SymbolIndex();
    IndexSymbols indexer = new IndexSymbols(projectInfo, cu, file, index);
    cu.accept(indexer);
    assertFalse(index.definitions.isEmpty());
    assertFalse(index.references.isEmpty());

    // Test PrintAST.Coder
    new PrintAST.Coder(projectInfo).rewrite();

    // Test LoggingASTVisitor fatal + snippet formatting (failAtEnd = true)
    LoggingASTVisitor visitor = new LoggingASTVisitor(cu, file) {
      @Override
      public boolean visit(MethodDeclaration node) {
        debug(node, "visiting %s", node.getName().getIdentifier());
        info(node, "info %s", node.getName().getIdentifier());
        warn(node, "warn %s", node.getName().getIdentifier());
        fatal(node, "Test failure on method %s", node.getName().getIdentifier());
        return false;
      }
    };
    visitor.setFailAtEnd(true);
    assertTrue(visitor.isFailAtEnd());
    CollectableException ex = assertThrows(CollectableException.class, () -> cu.accept(visitor));
    String msg = ex.getMessage();
    assertTrue(msg.contains("Sample.java"), "Expected file name in message: " + msg);
    assertTrue(msg.contains("Test failure on method add"), "Expected formatted error: " + msg);
    assertTrue(msg.contains("public int add"), "Expected source snippet: " + msg);

    // Test failAtEnd = false throws immediately
    LoggingASTVisitor immediateVisitor = new LoggingASTVisitor(cu, file) {
      @Override
      public boolean visit(MethodDeclaration node) {
        fatal(node, "Immediate failure");
        return false;
      }
    };
    immediateVisitor.setFailAtEnd(false);
    assertThrows(CollectableException.class, () -> cu.accept(immediateVisitor));

    // Test ASTUtil AST queries
    TypeDeclaration typeDecl = (TypeDeclaration) cu.types().get(0);
    MethodDeclaration methodDecl = typeDecl.getMethods()[0];
    ReturnStatement ret = (ReturnStatement) methodDecl.getBody().statements()
        .get(methodDecl.getBody().statements().size() - 1);
    assertTrue(ASTUtil.isExit(ret));
    assertFalse(ASTUtil.isEvaluable(ret.getExpression()));
    assertFalse(ASTUtil.isEvaluable(null));
    VariableDeclarationStatement varDecl = (VariableDeclarationStatement) methodDecl.getBody().statements().get(0);
    VariableDeclarationFragment frag = (VariableDeclarationFragment) varDecl.fragments().get(0);
    assertTrue(ASTUtil.isEvaluable(frag.getInitializer()));
    assertTrue(ASTUtil.isPrimitive(methodDecl.getReturnType2().resolveBinding()));
    assertFalse(ASTUtil.isPrimitive(typeDecl.resolveBinding()));
    assertNotNull(ASTUtil.newQualifiedName(cu.getAST(), "com", "example", "Foo"));
    ASTEditor.ASTMapping aligned = ASTUtil.align(cu, cu);
    assertTrue(aligned.errors.isEmpty());
    assertFalse(aligned.matches.isEmpty());
    assertNotNull(ASTUtil.format(source));
  }
}
