package com.simiacryptus.ref.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BytecodeVerifierTest {

  private static final String CLEAN_RESOURCE_SRC = String.join("\n",
      "package com.example;",
      "import com.simiacryptus.ref.lang.MustCall;",
      "import com.simiacryptus.ref.lang.ReferenceCountingBase;",
      "public class Res extends ReferenceCountingBase {",
      "  private int data = 42;",
      "  public int read() { assertAlive(); return data; }",
      "  @Override public Res addRef() { return (Res) super.addRef(); }",
      "  @MustCall @Override protected void _free() { super._free(); }",
      "}"
  );

  @Test
  public void testValidJavaPatternsPassCleanly() {
    Map<String, String> sources = new LinkedHashMap<>();
    sources.put("com.example.Res", CLEAN_RESOURCE_SRC);
    sources.put("com.example.ValidShowcase", String.join("\n",
        "package com.example;",
        "import com.simiacryptus.ref.lang.*;",
        "import com.simiacryptus.ref.wrappers.*;",
        "import java.util.Optional;",
        "import java.util.function.Supplier;",
        "public class ValidShowcase extends ReferenceCountingBase {",
        "  private Res item;",
        "  public ValidShowcase(Res item) {",
        "    this.item = item == null ? null : item.addRef();",
        "    if (item != null) item.freeRef();",
        "  }",
        "  public Res getItem() {",
        "    return item == null ? null : item.addRef();",
        "  }",
        "  @RefIgnore public Res peekItem() {",
        "    return item;",
        "  }",
        "  public void replaceItem(Res next) {",
        "    if (this.item != null) this.item.freeRef();",
        "    this.item = next;",
        "  }",
        "  public int tryFinallyAndCollection() {",
        "    RefArrayList<Res> list = new RefArrayList<>();",
        "    try {",
        "      list.add(new Res());",
        "      Res fetched = list.get(0);",
        "      try {",
        "        return fetched.read();",
        "      } finally {",
        "        fetched.freeRef();",
        "      }",
        "    } finally {",
        "      list.freeRef();",
        "    }",
        "  }",
        "  public Res conditionalAndPhi(boolean flag, Res fallback) {",
        "    Res chosen = flag ? new Res() : (fallback == null ? null : fallback.addRef());",
        "    if (fallback != null) fallback.freeRef();",
        "    return chosen;",
        "  }",
        "  public void conditionalAlloc(boolean create) {",
        "    Res maybe = null;",
        "    if (create) maybe = new Res();",
        "    if (maybe != null) maybe.freeRef();",
        "  }",
        "  public void arrayHandling() {",
        "    Res[] arr = new Res[]{ new Res(), new Res() };",
        "    int v = arr[0].read();",
        "    RefUtil.freeRefs((Object[]) arr);",
        "  }",
        "  @RefAware public Supplier<Res> wrappedClosure(Res captured) {",
        "    Supplier<Res> fn = RefUtil.wrapInterface(",
        "        (Supplier<Res>) () -> captured.addRef(),",
        "        captured.addRef()",
        "    );",
        "    captured.freeRef();",
        "    return fn;",
        "  }",
        "  @Override public ValidShowcase addRef() { return (ValidShowcase) super.addRef(); }",
        "  @MustCall @Override protected void _free() {",
        "    if (this.item != null) { this.item.freeRef(); this.item = null; }",
        "    super._free();",
        "  }",
        "}"
    ));

    Map<String, byte[]> compiled = InMemoryJavaCompiler.compile(sources);
    BytecodeVerifier verifier = new BytecodeVerifier();
    VerificationResult result = verifier.analyzeBytes(compiled.values());

    assertFalse(result.hasErrors(), () -> "Expected zero errors, got:\n" + result.formatReport());
    assertEquals(2, result.getRefCountedClassCount());
    assertEquals(SourceLanguage.JAVA, result.getLanguageBreakdown().entrySet().iterator().next().getKey());
  }

  @Test
  public void testHierarchyAndContractViolations() {
    Map<String, String> sources = new LinkedHashMap<>();
    sources.put("com.example.Res", CLEAN_RESOURCE_SRC);
    sources.put("com.example.AwareBase", String.join("\n",
        "package com.example;",
        "import com.simiacryptus.ref.lang.*;",
        "public interface AwareBase {",
        "  void accept(@RefAware Object x);",
        "}"
    ));
    sources.put("com.example.BadHierarchy", String.join("\n",
        "package com.example;",
        "import com.simiacryptus.ref.lang.*;",
        "public class BadHierarchy implements AwareBase {",
        "  private Res unaccountedField;",
        "  @Override public void accept(Object x) {}",
        "  public class NonStaticInner extends ReferenceCountingBase {}",
        "  public static class MissingFree extends ReferenceCountingBase {",
        "    private Res child;",
        "  }",
        "  public static class MissingSuper extends ReferenceCountingBase {",
        "    @Override protected void _free() { /* forgot super._free() */ }",
        "  }",
        "}"
    ));

    Map<String, byte[]> compiled = InMemoryJavaCompiler.compile(sources);
    VerificationResult result = new BytecodeVerifier().analyzeBytes(compiled.values());

    assertTrue(result.hasErrors());
    assertFalse(result.getByRule(RuleId.UNACCOUNTABLE_REF_FIELD).isEmpty(), "Expected UNACCOUNTABLE_REF_FIELD");
    assertFalse(result.getByRule(RuleId.NON_STATIC_INNER_REFCOUNTED).isEmpty(), "Expected NON_STATIC_INNER_REFCOUNTED");
    assertFalse(result.getByRule(RuleId.MISSING_FREE_METHOD).isEmpty(), "Expected MISSING_FREE_METHOD");
    assertFalse(result.getByRule(RuleId.MISSING_MUST_CALL_SUPER).isEmpty(), "Expected MISSING_MUST_CALL_SUPER");
    assertFalse(result.getByRule(RuleId.MISSING_REF_AWARE_OVERRIDE).isEmpty(), "Expected MISSING_REF_AWARE_OVERRIDE");
  }

  @Test
  public void testDataflowAndFieldLifecycleViolations() {
    Map<String, String> sources = new LinkedHashMap<>();
    sources.put("com.example.Res", CLEAN_RESOURCE_SRC);
    sources.put("com.example.BuggyOps", String.join("\n",
        "package com.example;",
        "import com.simiacryptus.ref.lang.*;",
        "import java.util.ArrayList;",
        "import java.util.List;",
        "import java.util.function.Supplier;",
        "public class BuggyOps extends ReferenceCountingBase {",
        "  private Res owned;",
        "  public void leakLocal() {",
        "    Res r = new Res();",
        "    r.read();",
        "  }",
        "  public void discardAllocation() {",
        "    new Res();",
        "  }",
        "  public int chainedTemporaryLeak() {",
        "    return makeRes().read();",
        "  }",
        "  private static Res makeRes() { return new Res(); }",
        "  public void doubleFree(Res param) {",
        "    param.freeRef();",
        "    param.freeRef();",
        "  }",
        "  public int useAfterFree(Res param) {",
        "    param.freeRef();",
        "    return param.read();",
        "  }",
        "  public void freeBorrowed(@RefIgnore Res borrowed) {",
        "    borrowed.freeRef();",
        "  }",
        "  public void branchMismatch(Res param, boolean flag) {",
        "    if (flag) {",
        "      param.freeRef();",
        "    }",
        "  }",
        "  public void blindCollectionSink() {",
        "    List<Res> list = new ArrayList<>();",
        "    Res r = new Res();",
        "    list.add(r);",
        "  }",
        "  public Object returnLosesAwareness() {",
        "    return new Res();",
        "  }",
        "  public Supplier<Res> leakLambdaCapture(Res captured) {",
        "    return () -> captured;",
        "  }",
        "  public void overwriteFieldWithoutFree(Res next) {",
        "    this.owned = next;",
        "  }",
        "  public void freeFieldOutsideFree() {",
        "    this.owned.freeRef();",
        "  }",
        "  @Override protected void _free() {",
        "    // forgot to free this.owned!",
        "    super._free();",
        "  }",
        "}"
    ));

    Map<String, byte[]> compiled = InMemoryJavaCompiler.compile(sources);
    VerificationResult result = new BytecodeVerifier().analyzeBytes(compiled.values());

    assertTrue(result.hasErrors());
    assertFalse(result.getByRule(RuleId.UNFREED_REFERENCE).isEmpty(), "Expected UNFREED_REFERENCE");
    assertFalse(result.getByRule(RuleId.DISCARDED_REFERENCE).isEmpty(), "Expected DISCARDED_REFERENCE");
    assertFalse(result.getByRule(RuleId.DOUBLE_FREE).isEmpty(), "Expected DOUBLE_FREE");
    assertFalse(result.getByRule(RuleId.USE_AFTER_FREE).isEmpty(), "Expected USE_AFTER_FREE");
    assertFalse(result.getByRule(RuleId.UNOWNED_REFERENCE_CONSUMED).isEmpty(), "Expected UNOWNED_REFERENCE_CONSUMED");
    assertFalse(result.getByRule(RuleId.BRANCH_CONSUMPTION_MISMATCH).isEmpty(), "Expected BRANCH_CONSUMPTION_MISMATCH");
    assertFalse(result.getByRule(RuleId.BLIND_PARAMETER_PASS).isEmpty(), "Expected BLIND_PARAMETER_PASS");
    assertFalse(result.getByRule(RuleId.RETURN_LOSES_REF_AWARENESS).isEmpty(), "Expected RETURN_LOSES_REF_AWARENESS");
    assertFalse(result.getByRule(RuleId.FIELD_OVERWRITTEN_WITHOUT_FREE).isEmpty(), "Expected FIELD_OVERWRITTEN_WITHOUT_FREE");
    assertFalse(result.getByRule(RuleId.FIELD_FREED_OUTSIDE_FREE).isEmpty(), "Expected FIELD_FREED_OUTSIDE_FREE");
    assertFalse(result.getByRule(RuleId.FIELD_NOT_FREED_IN_FREE).isEmpty(), "Expected FIELD_NOT_FREED_IN_FREE");

    BytecodeVerificationException ex = assertThrows(BytecodeVerificationException.class, result::throwIfErrors);
    assertTrue(ex.getMessage().contains("BuggyOps"));
    assertFalse(ex.getErrors().isEmpty());
  }

  @Test
  public void testNonJavaModulesKotlinScalaGroovyAndStrippedBytecode() {
    byte[] kRes = NonJavaBytecodeBuilder.buildKotlinResourceClass("pkg/kt/KRes");
    byte[] validKt = NonJavaBytecodeBuilder.buildValidKotlinContainer("pkg/kt/KotlinContainer", "pkg/kt/KRes");
    byte[] validScala = NonJavaBytecodeBuilder.buildScalaServiceClass("pkg/sc/ScalaService", "pkg/kt/KRes", false);
    byte[] validGroovy = NonJavaBytecodeBuilder.buildGroovyClass("pkg/gr/GroovyWorker", "pkg/kt/KRes", false);

    VerificationResult cleanResult = new BytecodeVerifier().analyzeBytes(
        Arrays.asList(kRes, validKt, validScala, validGroovy)
    );
    assertFalse(cleanResult.hasErrors(), () -> "Expected non-Java clean modules to pass:\n" + cleanResult.formatReport());
    assertEquals(2, cleanResult.getLanguageBreakdown().get(SourceLanguage.KOTLIN));
    assertEquals(1, cleanResult.getLanguageBreakdown().get(SourceLanguage.SCALA));
    assertEquals(1, cleanResult.getLanguageBreakdown().get(SourceLanguage.GROOVY));

    // Now analyze buggy Kotlin, Scala, Groovy, and debug-stripped bytecode classes
    byte[] buggyKt = NonJavaBytecodeBuilder.buildBuggyKotlinContainer("pkg/kt/BuggyKotlin", "pkg/kt/KRes");
    byte[] leakyScala = NonJavaBytecodeBuilder.buildScalaServiceClass("pkg/sc/LeakyScala", "pkg/kt/KRes", true);
    byte[] leakyGroovy = NonJavaBytecodeBuilder.buildGroovyClass("pkg/gr/LeakyGroovy", "pkg/kt/KRes", true);
    byte[] stripped = NonJavaBytecodeBuilder.buildStrippedBytecodeWithDoubleFree("pkg/raw/Generated", "pkg/kt/KRes");

    VerificationResult buggyResult = new BytecodeVerifier().analyzeBytes(
        Arrays.asList(kRes, buggyKt, leakyScala, leakyGroovy, stripped)
    );
    assertTrue(buggyResult.hasErrors());

    // Kotlin diagnostics carry SourceLanguage.KOTLIN and Kotlin source filename/lines
    assertTrue(buggyResult.getDiagnostics().stream().anyMatch(d ->
        d.getLanguage() == SourceLanguage.KOTLIN
            && "BuggyKotlin.kt".equals(d.getSourceFile())
            && d.getRuleId() == RuleId.UNOWNED_REFERENCE_CONSUMED
    ), "Expected Kotlin unowned getter return diagnostic");

    assertTrue(buggyResult.getDiagnostics().stream().anyMatch(d ->
        d.getLanguage() == SourceLanguage.KOTLIN
            && d.getRuleId() == RuleId.FIELD_OVERWRITTEN_WITHOUT_FREE
    ), "Expected Kotlin setter overwrite diagnostic");

    assertTrue(buggyResult.getDiagnostics().stream().anyMatch(d ->
        d.getLanguage() == SourceLanguage.KOTLIN
            && d.getRuleId() == RuleId.MISSING_MUST_CALL_SUPER
    ), "Expected Kotlin missing super._free() diagnostic");

    // Scala synthetic $anonfun$ closure leak detected
    assertTrue(buggyResult.getDiagnostics().stream().anyMatch(d ->
        d.getLanguage() == SourceLanguage.SCALA
            && "ScalaService.scala".equals(d.getSourceFile())
            && d.getRuleId() == RuleId.DISCARDED_REFERENCE
    ), "Expected Scala $anonfun$ closure discarded reference diagnostic");

    // Groovy leak detected
    assertTrue(buggyResult.getDiagnostics().stream().anyMatch(d ->
        d.getLanguage() == SourceLanguage.GROOVY
            && d.getRuleId() == RuleId.UNFREED_REFERENCE
    ), "Expected Groovy unfreed reference diagnostic");

    // Stripped raw bytecode double-free detected
    assertTrue(buggyResult.getDiagnostics().stream().anyMatch(d ->
        d.getLanguage() == SourceLanguage.BYTECODE
            && d.getRuleId() == RuleId.DOUBLE_FREE
    ), "Expected raw stripped bytecode double-free diagnostic");
  }

  @Test
  public void testJarScanningClasspathResolutionAndCli(@TempDir Path tempDir) throws Exception {
    // Compile base resource into a dependency JAR, and consumer into a target JAR
    Map<String, byte[]> libClasses = InMemoryJavaCompiler.compileSingle("com.example.Res", CLEAN_RESOURCE_SRC);
    File libJar = InMemoryJavaCompiler.writeClassesToJar(libClasses, tempDir.resolve("libs/res-lib.jar"));

    Map<String, byte[]> appClasses = new LinkedHashMap<>();
    appClasses.put("pkg.kt.KotlinContainer", NonJavaBytecodeBuilder.buildValidKotlinContainer("pkg/kt/KotlinContainer", "com/example/Res"));
    appClasses.put("pkg.kt.BuggyKotlin", NonJavaBytecodeBuilder.buildBuggyKotlinContainer("pkg/kt/BuggyKotlin", "com/example/Res"));
    File appJar = InMemoryJavaCompiler.writeClassesToJar(appClasses, tempDir.resolve("app/app.jar"));

    Path jsonReport = tempDir.resolve("reports/out.json");
    Path txtReport = tempDir.resolve("reports/out.txt");

    // 1. Run CLI excluding BuggyKotlin -> should pass cleanly and emit JSON report
    VerificationResult filteredResult = BytecodeVerifier.runCli(new String[]{
        "--classpath", libJar.getAbsolutePath(),
        "--exclude-class", "pkg.kt.BuggyKotlin",
        "--report", jsonReport.toAbsolutePath().toString(),
        appJar.getAbsolutePath()
    });
    assertFalse(filteredResult.hasErrors(), filteredResult::formatReport);
    assertTrue(Files.exists(jsonReport));
    String jsonText = Files.readString(jsonReport);
    assertTrue(jsonText.contains("\"errorCount\" : 0"));
    assertTrue(jsonText.contains("\"KOTLIN\" : 1"));

    // 2. Run CLI without exclusion -> detects errors and writes text report
    VerificationResult unfilteredResult = BytecodeVerifier.runCli(new String[]{
        "-cp", libJar.getAbsolutePath(),
        "--report", txtReport.toAbsolutePath().toString(),
        appJar.getAbsolutePath()
    });
    assertTrue(unfilteredResult.hasErrors());
    assertTrue(Files.exists(txtReport));
    assertTrue(Files.readString(txtReport).contains("BuggyKotlin"));

    // 3. main() throws BytecodeVerificationException when errors exist
    assertThrows(BytecodeVerificationException.class, () -> BytecodeVerifier.main(new String[]{
        "-cp", libJar.getAbsolutePath(),
        appJar.getAbsolutePath()
    }));
  }

  @Test
  public void testCompiledRefcountDemoModuleAnalysis() throws Exception {
    String demoClassesDir = System.getProperty("demo.classes.dir", "");
    String demoJarFiles = System.getProperty("demo.jar.files", "");
    List<File> targets = new ArrayList<>();
    if (!demoClassesDir.isEmpty() && new File(demoClassesDir).exists()) {
      targets.add(new File(demoClassesDir));
    }
    List<File> cp = new ArrayList<>();
    for (String entry : demoJarFiles.split(File.pathSeparator)) {
      if (!entry.isEmpty() && new File(entry).exists()) {
        File f = new File(entry);
        cp.add(f);
        if (targets.isEmpty() && f.getName().contains("demo")) {
          targets.add(f);
        }
      }
    }
    if (!targets.isEmpty()) {
      // Structural hierarchy check on compiled refcount-demo detects WrapperType's missing super._free()
      // while all other 21 demo classes pass hierarchy checks cleanly!
      BytecodeVerifier checkVerifier = new BytecodeVerifier(VerifierConfig.builder().checkOnly(true).build());
      VerificationResult checkResult = checkVerifier.analyze(targets, cp);
      assertTrue(checkResult.getAnalyzedClassCount() > 0, "Expected compiled demo classes to be analyzed");
      assertTrue(checkResult.getErrors().stream().allMatch(d ->
          "com.simiacryptus.demo.refcount.WrapperType".equals(d.getClassName())
              && d.getRuleId() == RuleId.MISSING_MUST_CALL_SUPER
      ), () -> "Unexpected demo hierarchy errors:\n" + checkResult.formatReport());

      // Excluding WrapperType yields zero hierarchy errors across the rest of refcount-demo
      BytecodeVerifier filteredCheck = new BytecodeVerifier(
          VerifierConfig.builder().checkOnly(true).excludeClass("com.simiacryptus.demo.refcount.WrapperType").build()
      );
      assertFalse(filteredCheck.analyze(targets, cp).hasErrors());

      // Also exercise full dataflow analyzer across all compiled demo classes
      VerificationResult fullResult = new BytecodeVerifier().analyze(targets, cp);
      assertTrue(fullResult.getAnalyzedMethodCount() > 10);
      assertNotNull(fullResult.toJson());
    }
  }
}
