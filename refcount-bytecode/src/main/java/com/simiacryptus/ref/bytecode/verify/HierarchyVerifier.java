/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.bytecode.verify;

import com.simiacryptus.ref.bytecode.Diagnostic;
import com.simiacryptus.ref.bytecode.RuleId;
import com.simiacryptus.ref.bytecode.Severity;
import com.simiacryptus.ref.bytecode.model.*;
import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@RefIgnore
public final class HierarchyVerifier {
  @Nonnull
  private final BytecodeTypeIndex typeIndex;

  public HierarchyVerifier(@Nonnull BytecodeTypeIndex typeIndex) {
    this.typeIndex = typeIndex;
  }

  public void verify(@Nonnull ClassMetadata cls, @Nonnull List<Diagnostic> out) {
    if (cls.isRefIgnore() || cls.isAnnotation() || cls.isModuleInfo() || cls.isSynthetic()) {
      return;
    }
    if (cls.getInternalName().endsWith("$WhenMappings")) {
      return;
    }

    ClassNode classNode = cls.getClassNode();
    int classLine = BytecodeInstructionUtil.findFirstClassLine(classNode);
    boolean isRefCountedClass = typeIndex.isReferenceCountingClass(cls.getInternalName());

    // 1. Non-static inner class implementing ReferenceCounting
    if (cls.isNonStaticInner() && isRefCountedClass) {
      out.add(new Diagnostic(
          RuleId.NON_STATIC_INNER_REFCOUNTED,
          Severity.ERROR,
          cls.getBinaryName(),
          null,
          null,
          cls.getSourceFile(),
          classLine,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Non-static inner class '%s' cannot implement ReferenceCounting (make it static or annotate with @RefIgnore)",
              cls.getBinaryName()
          )
      ));
    }

    // 2. Collect non-static, non-@RefIgnore reference-counted fields
    List<FieldMetadata> ownedRefFields = new ArrayList<>();
    for (FieldMetadata field : cls.getFields()) {
      if (field.isStatic() || field.isRefIgnore()) continue;
      // Skip outer 'this$0' synthetic pointer (already covered by inner-class check)
      if (field.isSynthetic() && field.getName().startsWith("this$")) continue;
      if (field.isRefAware() || typeIndex.isRefCountedType(field.getType(), field.getSignature())) {
        ownedRefFields.add(field);
      }
    }

    if (!isRefCountedClass && !ownedRefFields.isEmpty()) {
      // Anonymous interface / Kotlin Lambda / Scala Function classes may hold synthetic captured variables
      // when wrapped via RefUtil.wrapInterface or used as synchronous borrows; check non-closure classes strictly.
      if (!isAllowedAnonymousInterfaceClosure(cls)) {
        for (FieldMetadata field : ownedRefFields) {
          RuleId rule = cls.isAnonymousOrClosure() && field.isSynthetic()
              ? RuleId.UNSAFE_CLOSURE_CAPTURE
              : RuleId.UNACCOUNTABLE_REF_FIELD;
          out.add(new Diagnostic(
              rule,
              Severity.ERROR,
              cls.getBinaryName(),
              null,
              null,
              cls.getSourceFile(),
              classLine,
              cls.getLanguage(),
              cls.getOriginPath(),
              String.format(
                  "Unaccountable reference: field '%s' of reference-counted type '%s' declared in non-reference-counted class '%s'",
                  field.getName(),
                  field.getType().getClassName(),
                  cls.getBinaryName()
              )
          ));
        }
      }
    }

    // 3. Concrete ReferenceCountingBase subclass with ref-counted fields must declare _free()
    if (isRefCountedClass
        && !cls.isInterface()
        && !cls.isAbstract()
        && !ownedRefFields.isEmpty()
        && typeIndex.isReferenceCountingBaseSubclass(cls.getInternalName())) {
      MethodMetadata freeMethod = cls.findMethod("_free", "()V");
      if (freeMethod == null) {
        String fieldNames = ownedRefFields.stream()
            .map(FieldMetadata::getName)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        out.add(new Diagnostic(
            RuleId.MISSING_FREE_METHOD,
            Severity.ERROR,
            cls.getBinaryName(),
            "_free",
            "()V",
            cls.getSourceFile(),
            classLine,
            cls.getLanguage(),
            cls.getOriginPath(),
            String.format(
                "Class '%s' extends ReferenceCountingBase and owns reference-counted field(s) [%s] but does not override _free()",
                cls.getBinaryName(),
                fieldNames
            )
        ));
      }
    }

    // 4. Method-level @MustCall super checks and @RefAware override checks
    for (MethodMetadata method : cls.getMethods()) {
      if (method.isRefIgnore() || method.isBridge() || method.isSynthetic() || method.isStatic()) {
        continue;
      }
      if ("<init>".equals(method.getName()) || "<clinit>".equals(method.getName())) {
        continue;
      }
      List<MethodMetadata> supers = typeIndex.findSuperMethods(
          cls.getInternalName(), method.getName(), method.getDescriptor()
      );
      if (supers.isEmpty()) continue;

      MethodNode methodNode = method.getMethodNode();
      int methodLine = methodNode != null ? BytecodeInstructionUtil.findFirstLine(methodNode.instructions) : classLine;

      boolean superMustCall = supers.stream().anyMatch(MethodMetadata::isMustCall);
      if (superMustCall && methodNode != null && !method.isAbstract() && !method.isNative()) {
        if (!containsSuperMethodCall(cls, methodNode)) {
          out.add(new Diagnostic(
              RuleId.MISSING_MUST_CALL_SUPER,
              Severity.ERROR,
              cls.getBinaryName(),
              method.getName(),
              method.getDescriptor(),
              cls.getSourceFile(),
              methodLine,
              cls.getLanguage(),
              cls.getOriginPath(),
              String.format(
                  "Method '%s%s' overrides a @MustCall method without invoking super.%s(...)",
                  method.getName(),
                  method.getDescriptor(),
                  method.getName()
              )
          ));
        }
      }

      Type[] paramTypes = Type.getArgumentTypes(method.getDescriptor());
      List<String> paramSigs = SignatureUtil.extractMethodParameterSignatures(method.getSignature());
      for (int i = 0; i < paramTypes.length; i++) {
        if (method.isParameterRefAware(i) || method.isParameterRefIgnore(i)) {
          continue;
        }
        final int idx = i;
        boolean superHasRefAware = supers.stream().anyMatch(s -> s.isParameterRefAware(idx));
        if (superHasRefAware) {
          Type paramType = paramTypes[i];
          String pSig = i < paramSigs.size() ? paramSigs.get(i) : null;
          if (typeIndex.isRefCountedType(paramType, pSig)) {
            continue;
          }
          if (paramType.getSort() == Type.OBJECT) {
            ClassMetadata paramClass = typeIndex.resolveClass(paramType.getInternalName());
            if (paramClass != null && paramClass.isFinal()) {
              continue;
            }
          }
          out.add(new Diagnostic(
              RuleId.MISSING_REF_AWARE_OVERRIDE,
              Severity.ERROR,
              cls.getBinaryName(),
              method.getName(),
              method.getDescriptor(),
              cls.getSourceFile(),
              methodLine,
              cls.getLanguage(),
              cls.getOriginPath(),
              String.format(
                  "Parameter %d of overriding method '%s%s' omits @RefAware declared on super method",
                  i,
                  method.getName(),
                  method.getDescriptor()
              )
          ));
        }
      }
    }
  }

  private boolean isAllowedAnonymousInterfaceClosure(@Nonnull ClassMetadata cls) {
    if (!cls.isAnonymousOrClosure()) return false;
    if (BytecodeTypeIndex.INTERNAL_OBJECT.equals(cls.getSuperName()) && !cls.getInterfaces().isEmpty()) {
      return true;
    }
    return "kotlin/jvm/internal/Lambda".equals(cls.getSuperName())
        || (cls.getSuperName() != null && cls.getSuperName().startsWith("scala/runtime/AbstractFunction"));
  }

  private boolean containsSuperMethodCall(@Nonnull ClassMetadata cls, @Nonnull MethodNode methodNode) {
    if (methodNode.instructions == null) return false;
    for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
      if (insn instanceof MethodInsnNode) {
        MethodInsnNode call = (MethodInsnNode) insn;
        if (call.getOpcode() == Opcodes.INVOKESPECIAL
            && methodNode.name.equals(call.name)
            && methodNode.desc.equals(call.desc)
            && !cls.getInternalName().equals(call.owner)) {
          return true;
        }
      }
    }
    return false;
  }
}
