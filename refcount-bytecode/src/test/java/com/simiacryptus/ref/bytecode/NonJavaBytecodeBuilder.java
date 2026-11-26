package com.simiacryptus.ref.bytecode;

import com.simiacryptus.ref.lang.MustCall;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.objectweb.asm.*;

/**
 * Generates realistic non-Java JVM bytecode (Kotlin, Scala, Groovy, and debug-stripped bytecode) using ASM
 * to test cross-language reference-counting verification without requiring external language compilers at runtime.
 */
final class NonJavaBytecodeBuilder implements Opcodes {
  static final String BASE_INTERNAL = Type.getInternalName(ReferenceCountingBase.class);
  static final String MUST_CALL_DESC = Type.getDescriptor(MustCall.class);
  static final String KOTLIN_METADATA_DESC = "Lkotlin/Metadata;";
  static final String SCALA_SIG_DESC = "Lscala/reflect/ScalaSignature;";

  private NonJavaBytecodeBuilder() {
  }

  /**
   * Builds a simple concrete ReferenceCountingBase subclass {@code pkg/KRes} marked with {@code @kotlin.Metadata}.
   */
  static byte[] buildKotlinResourceClass(String internalName) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(V21, ACC_PUBLIC | ACC_SUPER, internalName, null, BASE_INTERNAL, null);
    cw.visitSource("KRes.kt", null);
    cw.visitAnnotation(KOTLIN_METADATA_DESC, true).visitEnd();

    // public <init>()V
    MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    init.visitCode();
    Label l0 = new Label();
    init.visitLabel(l0);
    init.visitLineNumber(5, l0);
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, BASE_INTERNAL, "<init>", "()V", false);
    init.visitInsn(RETURN);
    init.visitMaxs(0, 0);
    init.visitEnd();

    // public final void touch()
    MethodVisitor touch = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "touch", "()V", null, null);
    touch.visitCode();
    Label lt = new Label();
    touch.visitLabel(lt);
    touch.visitLineNumber(8, lt);
    touch.visitVarInsn(ALOAD, 0);
    touch.visitMethodInsn(INVOKEVIRTUAL, internalName, "assertAlive", "()Z", false);
    touch.visitInsn(POP);
    touch.visitInsn(RETURN);
    touch.visitMaxs(0, 0);
    touch.visitEnd();

    // public KRes addRef()
    MethodVisitor addRef = cw.visitMethod(ACC_PUBLIC, "addRef", "()L" + internalName + ";", null, null);
    addRef.visitCode();
    addRef.visitVarInsn(ALOAD, 0);
    addRef.visitMethodInsn(INVOKESPECIAL, BASE_INTERNAL, "addRef", "()Lcom/simiacryptus/ref/lang/ReferenceCounting;", false);
    addRef.visitTypeInsn(CHECKCAST, internalName);
    addRef.visitInsn(ARETURN);
    addRef.visitMaxs(0, 0);
    addRef.visitEnd();

    // synthetic bridge ReferenceCounting addRef()
    MethodVisitor bridge = cw.visitMethod(
        ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
        "addRef",
        "()Lcom/simiacryptus/ref/lang/ReferenceCounting;",
        null,
        null
    );
    bridge.visitCode();
    bridge.visitVarInsn(ALOAD, 0);
    bridge.visitMethodInsn(INVOKEVIRTUAL, internalName, "addRef", "()L" + internalName + ";", false);
    bridge.visitInsn(ARETURN);
    bridge.visitMaxs(0, 0);
    bridge.visitEnd();

    cw.visitEnd();
    return cw.toByteArray();
  }

  /**
   * Builds a valid idiomatic Kotlin container class using:
   * - {@code Intrinsics.checkNotNullParameter(child, "child")}
   * - Kotlin safe-call {@code this.child?.freeRef()} in {@code _free()}
   * - Kotlin swap-and-free setter {@code val old = this.child; this.child = next; old?.freeRef()}
   * - Kotlin getter {@code this.child?.addRef()}
   * - Synthetic {@code update$default} method with bitmask guard
   */
  static byte[] buildValidKotlinContainer(String containerName, String resName) {
    String resDesc = "L" + resName + ";";
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(V21, ACC_PUBLIC | ACC_SUPER, containerName, null, BASE_INTERNAL, null);
    cw.visitSource("KotlinContainer.kt", null);
    cw.visitAnnotation(KOTLIN_METADATA_DESC, true).visitEnd();

    cw.visitField(ACC_PRIVATE, "child", resDesc, null, null).visitEnd();

    // Constructor: KotlinContainer(child: KRes) { Intrinsics.checkNotNullParameter(child, "child"); super(); this.child = child }
    MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + resDesc + ")V", null, null);
    init.visitCode();
    Label lInit = new Label();
    init.visitLabel(lInit);
    init.visitLineNumber(10, lInit);
    init.visitVarInsn(ALOAD, 1);
    init.visitLdcInsn("child");
    init.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkNotNullParameter", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, BASE_INTERNAL, "<init>", "()V", false);
    init.visitVarInsn(ALOAD, 0);
    init.visitVarInsn(ALOAD, 1);
    init.visitFieldInsn(PUTFIELD, containerName, "child", resDesc);
    init.visitInsn(RETURN);
    init.visitMaxs(0, 0);
    init.visitEnd();

    // Getter: getChild(): KRes? = this.child?.addRef()
    MethodVisitor getter = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "getChild", "()" + resDesc, null, null);
    getter.visitCode();
    Label lg0 = new Label();
    Label lgNull = new Label();
    Label lgEnd = new Label();
    getter.visitLabel(lg0);
    getter.visitLineNumber(15, lg0);
    getter.visitVarInsn(ALOAD, 0);
    getter.visitFieldInsn(GETFIELD, containerName, "child", resDesc);
    getter.visitInsn(DUP);
    getter.visitJumpInsn(IFNULL, lgNull);
    getter.visitMethodInsn(INVOKEVIRTUAL, resName, "addRef", "()" + resDesc, false);
    getter.visitJumpInsn(GOTO, lgEnd);
    getter.visitLabel(lgNull);
    getter.visitInsn(POP);
    getter.visitInsn(ACONST_NULL);
    getter.visitLabel(lgEnd);
    getter.visitInsn(ARETURN);
    getter.visitMaxs(0, 0);
    getter.visitEnd();

    // Setter: setChild(next: KRes?) { val old = this.child; this.child = next; old?.freeRef() }
    MethodVisitor setter = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "setChild", "(" + resDesc + ")V", null, null);
    setter.visitCode();
    Label ls0 = new Label();
    Label lsNull = new Label();
    Label lsEnd = new Label();
    setter.visitLabel(ls0);
    setter.visitLineNumber(20, ls0);
    setter.visitVarInsn(ALOAD, 0);
    setter.visitFieldInsn(GETFIELD, containerName, "child", resDesc);
    setter.visitVarInsn(ASTORE, 2);
    setter.visitVarInsn(ALOAD, 0);
    setter.visitVarInsn(ALOAD, 1);
    setter.visitFieldInsn(PUTFIELD, containerName, "child", resDesc);
    setter.visitVarInsn(ALOAD, 2);
    setter.visitInsn(DUP);
    setter.visitJumpInsn(IFNULL, lsNull);
    setter.visitMethodInsn(INVOKEVIRTUAL, resName, "freeRef", "()I", false);
    setter.visitInsn(POP);
    setter.visitJumpInsn(GOTO, lsEnd);
    setter.visitLabel(lsNull);
    setter.visitInsn(POP);
    setter.visitLabel(lsEnd);
    setter.visitInsn(RETURN);
    setter.visitMaxs(0, 0);
    setter.visitEnd();

    // Synthetic Kotlin default-arg stub: setChild$default(self, next, mask, marker)
    MethodVisitor defStub = cw.visitMethod(
        ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC,
        "setChild$default",
        "(L" + containerName + ";" + resDesc + "ILjava/lang/Object;)V",
        null,
        null
    );
    defStub.visitCode();
    Label ldSkip = new Label();
    defStub.visitVarInsn(ILOAD, 2);
    defStub.visitInsn(ICONST_1);
    defStub.visitInsn(IAND);
    defStub.visitJumpInsn(IFEQ, ldSkip);
    defStub.visitInsn(ACONST_NULL);
    defStub.visitVarInsn(ASTORE, 1);
    defStub.visitLabel(ldSkip);
    defStub.visitVarInsn(ALOAD, 0);
    defStub.visitVarInsn(ALOAD, 1);
    defStub.visitMethodInsn(INVOKEVIRTUAL, containerName, "setChild", "(" + resDesc + ")V", false);
    defStub.visitInsn(RETURN);
    defStub.visitMaxs(0, 0);
    defStub.visitEnd();

    // protected void _free() { this.child?.freeRef(); this.child = null; super._free() }
    MethodVisitor free = cw.visitMethod(ACC_PROTECTED, "_free", "()V", null, null);
    free.visitAnnotation(MUST_CALL_DESC, true).visitEnd();
    free.visitCode();
    Label lf0 = new Label();
    Label lfNull = new Label();
    Label lfAfter = new Label();
    free.visitLabel(lf0);
    free.visitLineNumber(30, lf0);
    free.visitVarInsn(ALOAD, 0);
    free.visitFieldInsn(GETFIELD, containerName, "child", resDesc);
    free.visitInsn(DUP);
    free.visitJumpInsn(IFNULL, lfNull);
    free.visitMethodInsn(INVOKEVIRTUAL, resName, "freeRef", "()I", false);
    free.visitInsn(POP);
    free.visitJumpInsn(GOTO, lfAfter);
    free.visitLabel(lfNull);
    free.visitInsn(POP);
    free.visitLabel(lfAfter);
    free.visitVarInsn(ALOAD, 0);
    free.visitInsn(ACONST_NULL);
    free.visitFieldInsn(PUTFIELD, containerName, "child", resDesc);
    free.visitVarInsn(ALOAD, 0);
    free.visitMethodInsn(INVOKESPECIAL, BASE_INTERNAL, "_free", "()V", false);
    free.visitInsn(RETURN);
    free.visitMaxs(0, 0);
    free.visitEnd();

    cw.visitEnd();
    return cw.toByteArray();
  }

  /**
   * Builds a buggy Kotlin class with:
   * 1) Property getter returning {@code this.child} directly without {@code addRef()}
   * 2) Property setter overwriting {@code this.child} without freeing previous value
   * 3) Method allocating {@code KRes} and never freeing it
   */
  static byte[] buildBuggyKotlinContainer(String containerName, String resName) {
    String resDesc = "L" + resName + ";";
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(V21, ACC_PUBLIC | ACC_SUPER, containerName, null, BASE_INTERNAL, null);
    cw.visitSource("BuggyKotlin.kt", null);
    cw.visitAnnotation(KOTLIN_METADATA_DESC, true).visitEnd();

    cw.visitField(ACC_PRIVATE, "child", resDesc, null, null).visitEnd();

    MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    init.visitCode();
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, BASE_INTERNAL, "<init>", "()V", false);
    init.visitInsn(RETURN);
    init.visitMaxs(0, 0);
    init.visitEnd();

    // Bug 1: getChild() returns unowned field without addRef()
    MethodVisitor badGetter = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "getChild", "()" + resDesc, null, null);
    badGetter.visitCode();
    Label l1 = new Label();
    badGetter.visitLabel(l1);
    badGetter.visitLineNumber(12, l1);
    badGetter.visitVarInsn(ALOAD, 0);
    badGetter.visitFieldInsn(GETFIELD, containerName, "child", resDesc);
    badGetter.visitInsn(ARETURN);
    badGetter.visitMaxs(0, 0);
    badGetter.visitEnd();

    // Bug 2: setChild(next) overwrites this.child without freeing old reference
    MethodVisitor badSetter = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "setChild", "(" + resDesc + ")V", null, null);
    badSetter.visitCode();
    Label l2 = new Label();
    badSetter.visitLabel(l2);
    badSetter.visitLineNumber(18, l2);
    badSetter.visitVarInsn(ALOAD, 0);
    badSetter.visitVarInsn(ALOAD, 1);
    badSetter.visitFieldInsn(PUTFIELD, containerName, "child", resDesc);
    badSetter.visitInsn(RETURN);
    badSetter.visitMaxs(0, 0);
    badSetter.visitEnd();

    // Bug 3: leakTemp() allocates KRes and never calls freeRef()
    MethodVisitor leak = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "leakTemp", "()V", null, null);
    leak.visitCode();
    Label l3 = new Label();
    leak.visitLabel(l3);
    leak.visitLineNumber(25, l3);
    leak.visitTypeInsn(NEW, resName);
    leak.visitInsn(DUP);
    leak.visitMethodInsn(INVOKESPECIAL, resName, "<init>", "()V", false);
    leak.visitVarInsn(ASTORE, 1);
    leak.visitVarInsn(ALOAD, 1);
    leak.visitMethodInsn(INVOKEVIRTUAL, resName, "touch", "()V", false);
    Label lEnd = new Label();
    leak.visitLabel(lEnd);
    leak.visitLineNumber(27, lEnd);
    leak.visitInsn(RETURN);
    leak.visitLocalVariable("leakedVal", resDesc, null, l3, lEnd, 1);
    leak.visitMaxs(0, 0);
    leak.visitEnd();

    // Bug 4: _free() omits super._free() AND omits freeing child!
    MethodVisitor free = cw.visitMethod(ACC_PROTECTED, "_free", "()V", null, null);
    free.visitCode();
    Label l4 = new Label();
    free.visitLabel(l4);
    free.visitLineNumber(32, l4);
    free.visitInsn(RETURN);
    free.visitMaxs(0, 0);
    free.visitEnd();

    cw.visitEnd();
    return cw.toByteArray();
  }

  /**
   * Builds a Scala object/service class ({@code @scala.reflect.ScalaSignature}, {@code ScalaService.scala})
   * with both a valid method using statement-style {@code addRef()} and a leaky {@code $anonfun$} closure method.
   */
  static byte[] buildScalaServiceClass(String serviceName, String resName, boolean includeLeakyClosure) {
    String resDesc = "L" + resName + ";";
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(V21, ACC_PUBLIC | ACC_SUPER, serviceName, null, "java/lang/Object", null);
    cw.visitSource("ScalaService.scala", null);
    cw.visitAnnotation(SCALA_SIG_DESC, true).visitEnd();

    // <init>
    MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    init.visitCode();
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    init.visitInsn(RETURN);
    init.visitMaxs(0, 0);
    init.visitEnd();

    // Valid method using statement-style r.addRef(); consume(r); consume(r);
    MethodVisitor sink = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "consumeOne", "(" + resDesc + ")V", null, null);
    sink.visitCode();
    sink.visitVarInsn(ALOAD, 0);
    sink.visitMethodInsn(INVOKESTATIC, "com/simiacryptus/ref/lang/RefUtil", "freeRef", "(Ljava/lang/Object;)V", false);
    sink.visitInsn(RETURN);
    sink.visitMaxs(0, 0);
    sink.visitEnd();

    MethodVisitor share = cw.visitMethod(ACC_PUBLIC, "shareTwice", "()V", null, null);
    share.visitCode();
    Label ls = new Label();
    share.visitLabel(ls);
    share.visitLineNumber(14, ls);
    share.visitTypeInsn(NEW, resName);
    share.visitInsn(DUP);
    share.visitMethodInsn(INVOKESPECIAL, resName, "<init>", "()V", false);
    share.visitVarInsn(ASTORE, 1);
    // Statement-style r.addRef(); (pops returned alias)
    share.visitVarInsn(ALOAD, 1);
    share.visitMethodInsn(INVOKEVIRTUAL, resName, "addRef", "()" + resDesc, false);
    share.visitInsn(POP);
    // Scala BoxesRunTime.equals(r, r) non-consuming intrinsic check
    share.visitVarInsn(ALOAD, 1);
    share.visitVarInsn(ALOAD, 1);
    share.visitMethodInsn(INVOKESTATIC, "scala/runtime/BoxesRunTime", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
    share.visitInsn(POP);
    // Consume twice!
    share.visitVarInsn(ALOAD, 1);
    share.visitMethodInsn(INVOKESTATIC, serviceName, "consumeOne", "(" + resDesc + ")V", false);
    share.visitVarInsn(ALOAD, 1);
    share.visitMethodInsn(INVOKESTATIC, serviceName, "consumeOne", "(" + resDesc + ")V", false);
    share.visitInsn(RETURN);
    share.visitMaxs(0, 0);
    share.visitEnd();

    if (includeLeakyClosure) {
      // Synthetic Scala closure $anonfun$run$1 that allocates a resource and drops it without freeRef
      MethodVisitor anon = cw.visitMethod(
          ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC,
          "$anonfun$run$1",
          "()V",
          null,
          null
      );
      anon.visitCode();
      Label la = new Label();
      anon.visitLabel(la);
      anon.visitLineNumber(42, la);
      anon.visitTypeInsn(NEW, resName);
      anon.visitInsn(DUP);
      anon.visitMethodInsn(INVOKESPECIAL, resName, "<init>", "()V", false);
      anon.visitInsn(POP);
      anon.visitInsn(RETURN);
      anon.visitMaxs(0, 0);
      anon.visitEnd();
    }

    cw.visitEnd();
    return cw.toByteArray();
  }

  /**
   * Builds a Groovy class ({@code implements groovy/lang/GroovyObject}) and a debug-stripped raw bytecode class.
   */
  static byte[] buildGroovyClass(String internalName, String resName, boolean leak) {
    String resDesc = "L" + resName + ";";
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(V21, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", new String[]{"groovy/lang/GroovyObject"});
    cw.visitSource("GroovyWorker.groovy", null);

    // Synthetic Groovy $getCallSiteArray helper (should be skipped cleanly)
    MethodVisitor callSite = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "$getCallSiteArray", "()[Ljava/lang/Object;", null, null);
    callSite.visitCode();
    callSite.visitInsn(ICONST_0);
    callSite.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    callSite.visitInsn(ARETURN);
    callSite.visitMaxs(0, 0);
    callSite.visitEnd();

    MethodVisitor run = cw.visitMethod(ACC_PUBLIC, "execute", "()V", null, null);
    run.visitCode();
    Label l0 = new Label();
    run.visitLabel(l0);
    run.visitLineNumber(9, l0);
    run.visitTypeInsn(NEW, resName);
    run.visitInsn(DUP);
    run.visitMethodInsn(INVOKESPECIAL, resName, "<init>", "()V", false);
    run.visitVarInsn(ASTORE, 1);
    if (!leak) {
      run.visitVarInsn(ALOAD, 1);
      run.visitMethodInsn(INVOKEVIRTUAL, resName, "freeRef", "()I", false);
      run.visitInsn(POP);
    }
    run.visitInsn(RETURN);
    run.visitMaxs(0, 0);
    run.visitEnd();

    cw.visitEnd();
    return cw.toByteArray();
  }

  /**
   * Builds a class with NO SourceFile and NO LineNumberTable / LocalVariableTable (e.g. obfuscated or runtime-generated bytecode).
   */
  static byte[] buildStrippedBytecodeWithDoubleFree(String internalName, String resName) {
    String resDesc = "L" + resName + ";";
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(V21, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", null);

    MethodVisitor m = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "doubleFree", "(" + resDesc + ")V", null, null);
    m.visitCode();
    m.visitVarInsn(ALOAD, 0);
    m.visitMethodInsn(INVOKEVIRTUAL, resName, "freeRef", "()I", false);
    m.visitInsn(POP);
    m.visitVarInsn(ALOAD, 0);
    m.visitMethodInsn(INVOKEVIRTUAL, resName, "freeRef", "()I", false);
    m.visitInsn(POP);
    m.visitInsn(RETURN);
    m.visitMaxs(0, 0);
    m.visitEnd();

    cw.visitEnd();
    return cw.toByteArray();
  }
}
