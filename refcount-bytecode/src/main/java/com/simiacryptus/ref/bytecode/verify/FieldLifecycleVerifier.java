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
import com.simiacryptus.ref.bytecode.model.BytecodeTypeIndex;
import com.simiacryptus.ref.bytecode.model.ClassMetadata;
import com.simiacryptus.ref.bytecode.model.FieldMetadata;
import com.simiacryptus.ref.bytecode.model.MethodMetadata;
import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Verifies reference-counted instance field lifecycles across {@code _free()}, constructors, and mutator methods.
 */
@RefIgnore
public final class FieldLifecycleVerifier {
  @Nonnull
  private final BytecodeTypeIndex typeIndex;

  public FieldLifecycleVerifier(@Nonnull BytecodeTypeIndex typeIndex) {
    this.typeIndex = typeIndex;
  }

  public void verify(@Nonnull ClassMetadata cls, @Nonnull List<Diagnostic> out) {
    if (cls.isRefIgnore() || cls.isInterface() || cls.isAnnotation() || cls.isModuleInfo() || cls.isSynthetic()) {
      return;
    }
    if (!typeIndex.isReferenceCountingClass(cls.getInternalName())) {
      return;
    }

    Map<String, FieldMetadata> refFields = new LinkedHashMap<>();
    for (FieldMetadata field : cls.getFields()) {
      if (field.isStatic() || field.isRefIgnore()) continue;
      if (field.isSynthetic() && field.getName().startsWith("this$")) continue;
      if (field.isRefAware() || typeIndex.isRefCountedType(field.getType(), field.getSignature())) {
        refFields.put(field.getName(), field);
      }
    }
    if (refFields.isEmpty()) {
      return;
    }

    for (MethodMetadata methodMeta : cls.getMethods()) {
      MethodNode method = methodMeta.getMethodNode();
      if (method == null || methodMeta.isRefIgnore() || methodMeta.isStatic()) continue;
      if (methodMeta.isAbstract() || methodMeta.isNative() || methodMeta.isBridge() || methodMeta.isSynthetic()) {
        continue;
      }
      if ("_free".equals(methodMeta.getName()) && "()V".equals(methodMeta.getDescriptor())) {
        verifyFreeMethod(cls, method, refFields, out);
      } else if ("<init>".equals(methodMeta.getName())) {
        verifyConstructorFieldWrites(cls, method, refFields, out);
      } else if (!"<clinit>".equals(methodMeta.getName())) {
        verifyRegularMethodFieldOps(cls, method, refFields, out);
      }
    }
  }

  private void verifyFreeMethod(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull Map<String, FieldMetadata> refFields,
      @Nonnull List<Diagnostic> out
  ) {
    Set<String> freedFields = findFieldsFreedInMethod(cls, method, refFields.keySet());
    int methodLine = BytecodeInstructionUtil.findFirstLine(method.instructions);
    for (FieldMetadata field : refFields.values()) {
      if (!freedFields.contains(field.getName())) {
        out.add(new Diagnostic(
            RuleId.FIELD_NOT_FREED_IN_FREE,
            Severity.ERROR,
            cls.getBinaryName(),
            method.name,
            method.desc,
            cls.getSourceFile(),
            methodLine,
            cls.getLanguage(),
            cls.getOriginPath(),
            String.format(
                "Reference-counted field '%s' (%s) is not freed in _free()",
                field.getName(),
                field.getType().getClassName()
            )
        ));
      }
    }
  }

  private void verifyConstructorFieldWrites(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull Map<String, FieldMetadata> refFields,
      @Nonnull List<Diagnostic> out
  ) {
    // In <init>, fields start uninitialized (null). A second overwrite without freeing the first value leaks.
    Set<String> initializedNonNull = new HashSet<>();
    Set<String> freedSinceInit = new HashSet<>();
    Set<String> savedToLocalAndFreed = findFieldsSavedToLocalAndFreed(cls, method, refFields.keySet());
    Set<String> freedDirectly = findFieldsFreedInMethod(cls, method, refFields.keySet());

    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
      String freedField = extractDirectFieldFree(cls, method, insn, refFields.keySet());
      if (freedField != null) {
        freedSinceInit.add(freedField);
      }
      if (insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.PUTFIELD) {
        FieldInsnNode put = (FieldInsnNode) insn;
        if (cls.getInternalName().equals(put.owner) && refFields.containsKey(put.name)) {
          boolean assigningNull = isPreviousNullConst(insn);
          if (initializedNonNull.contains(put.name)
              && !freedSinceInit.contains(put.name)
              && !freedDirectly.contains(put.name)
              && !savedToLocalAndFreed.contains(put.name)) {
            int line = BytecodeInstructionUtil.getLineNumber(method.instructions, insn);
            out.add(new Diagnostic(
                RuleId.FIELD_OVERWRITTEN_WITHOUT_FREE,
                Severity.ERROR,
                cls.getBinaryName(),
                method.name,
                method.desc,
                cls.getSourceFile(),
                line,
                cls.getLanguage(),
                cls.getOriginPath(),
                String.format(
                    "Constructor overwrites already-initialized reference-counted field '%s' without freeing previous reference",
                    put.name
                )
            ));
          }
          if (assigningNull) {
            initializedNonNull.remove(put.name);
            freedSinceInit.remove(put.name);
          } else {
            initializedNonNull.add(put.name);
            freedSinceInit.remove(put.name);
          }
        }
      }
    }
  }

  private void verifyRegularMethodFieldOps(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull Map<String, FieldMetadata> refFields,
      @Nonnull List<Diagnostic> out
  ) {
    Set<String> freedFields = findFieldsFreedInMethod(cls, method, refFields.keySet());
    Set<String> savedToLocalAndFreed = findFieldsSavedToLocalAndFreed(cls, method, refFields.keySet());
    Set<String> writtenFields = new HashSet<>();

    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
      if (insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.PUTFIELD) {
        FieldInsnNode put = (FieldInsnNode) insn;
        if (cls.getInternalName().equals(put.owner) && refFields.containsKey(put.name)) {
          writtenFields.add(put.name);
          if (!freedFields.contains(put.name) && !savedToLocalAndFreed.contains(put.name)) {
            int line = BytecodeInstructionUtil.getLineNumber(method.instructions, insn);
            out.add(new Diagnostic(
                RuleId.FIELD_OVERWRITTEN_WITHOUT_FREE,
                Severity.ERROR,
                cls.getBinaryName(),
                method.name,
                method.desc,
                cls.getSourceFile(),
                line,
                cls.getLanguage(),
                cls.getOriginPath(),
                String.format(
                    "Field '%s' is overwritten in '%s' without freeing its previous reference",
                    put.name,
                    method.name
                )
            ));
          }
        }
      }
    }

    // If a regular method frees an instance field without setting it to null or reassigning it, it leaves a dangling freed pointer
    for (String freed : freedFields) {
      if (!writtenFields.contains(freed)) {
        int line = findFieldFreeLine(cls, method, freed);
        out.add(new Diagnostic(
            RuleId.FIELD_FREED_OUTSIDE_FREE,
            Severity.ERROR,
            cls.getBinaryName(),
            method.name,
            method.desc,
            cls.getSourceFile(),
            line,
            cls.getLanguage(),
            cls.getOriginPath(),
            String.format(
                "Reference-counted field '%s' is freed in '%s' without nulling or reassigning the field",
                freed,
                method.name
            )
        ));
      }
    }
  }

  @Nonnull
  private Set<String> findFieldsFreedInMethod(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull Set<String> candidateFields
  ) {
    Set<String> freed = new HashSet<>();
    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
      String direct = extractDirectFieldFree(cls, method, insn, candidateFields);
      if (direct != null) {
        freed.add(direct);
      }
    }
    freed.addAll(findFieldsSavedToLocalAndFreed(cls, method, candidateFields));
    return freed;
  }

  /**
   * Detects both Java/autocoder {@code this.f.freeRef()} / {@code RefUtil.freeRef(this.f)} and
   * Kotlin safe-call {@code this.f?.freeRef()} ({@code GETFIELD f; DUP; IFNULL L; INVOKEVIRTUAL freeRef}).
   */
  @Nullable
  private String extractDirectFieldFree(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull AbstractInsnNode insn,
      @Nonnull Set<String> candidateFields
  ) {
    if (!(insn instanceof MethodInsnNode)) return null;
    MethodInsnNode call = (MethodInsnNode) insn;
    if (!BytecodeInstructionUtil.isInstanceFreeRef(call) && !BytecodeInstructionUtil.isStaticFreeRef(call)) {
      return null;
    }
    AbstractInsnNode prev = previousMeaningful(insn.getPrevious());
    // Skip optional CHECKCAST, DUP, IFNULL/IFNONNULL emitted by Kotlin ?.freeRef()
    int hops = 0;
    while (prev != null && hops < 5) {
      if (prev instanceof FieldInsnNode && prev.getOpcode() == Opcodes.GETFIELD) {
        FieldInsnNode get = (FieldInsnNode) prev;
        if (typeIndex.derivesFrom(cls.getInternalName(), get.owner) && candidateFields.contains(get.name)) {
          AbstractInsnNode beforeGet = previousMeaningful(prev.getPrevious());
          if (beforeGet instanceof VarInsnNode
              && beforeGet.getOpcode() == Opcodes.ALOAD
              && ((VarInsnNode) beforeGet).var == 0) {
            return get.name;
          }
        }
        break;
      }
      if (prev.getOpcode() == Opcodes.CHECKCAST
          || prev.getOpcode() == Opcodes.DUP
          || prev instanceof JumpInsnNode) {
        prev = previousMeaningful(prev.getPrevious());
        hops++;
      } else {
        break;
      }
    }
    return null;
  }

  /**
   * Detects idiomatic Kotlin/Java swap-and-free patterns:
   * {@code val old = this.f; this.f = newVal; old?.freeRef()}.
   */
  @Nonnull
  private Set<String> findFieldsSavedToLocalAndFreed(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull Set<String> candidateFields
  ) {
    Map<Integer, String> slotToField = new HashMap<>();
    Set<String> freed = new HashSet<>();
    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
      if (insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.GETFIELD) {
        FieldInsnNode get = (FieldInsnNode) insn;
        if (typeIndex.derivesFrom(cls.getInternalName(), get.owner) && candidateFields.contains(get.name)) {
          AbstractInsnNode before = previousMeaningful(insn.getPrevious());
          AbstractInsnNode after = nextMeaningful(insn.getNext());
          if (before instanceof VarInsnNode && before.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) before).var == 0
              && after instanceof VarInsnNode && after.getOpcode() == Opcodes.ASTORE) {
            slotToField.put(((VarInsnNode) after).var, get.name);
          }
        }
      } else if (insn instanceof MethodInsnNode) {
        MethodInsnNode call = (MethodInsnNode) insn;
        if (BytecodeInstructionUtil.isInstanceFreeRef(call) || BytecodeInstructionUtil.isStaticFreeRef(call)) {
          AbstractInsnNode prev = previousMeaningful(insn.getPrevious());
          int hops = 0;
          while (prev != null && hops < 5) {
            if (prev instanceof VarInsnNode && prev.getOpcode() == Opcodes.ALOAD) {
              String fieldName = slotToField.get(((VarInsnNode) prev).var);
              if (fieldName != null) {
                freed.add(fieldName);
              }
              break;
            }
            if (prev.getOpcode() == Opcodes.CHECKCAST
                || prev.getOpcode() == Opcodes.DUP
                || prev instanceof JumpInsnNode) {
              prev = previousMeaningful(prev.getPrevious());
              hops++;
            } else {
              break;
            }
          }
        }
      }
    }
    return freed;
  }

  private int findFieldFreeLine(@Nonnull ClassMetadata cls, @Nonnull MethodNode method, @Nonnull String fieldName) {
    Set<String> single = Collections.singleton(fieldName);
    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
      if (fieldName.equals(extractDirectFieldFree(cls, method, insn, single))) {
        return BytecodeInstructionUtil.getLineNumber(method.instructions, insn);
      }
    }
    return BytecodeInstructionUtil.findFirstLine(method.instructions);
  }

  private static boolean isPreviousNullConst(@Nonnull AbstractInsnNode putFieldInsn) {
    AbstractInsnNode prev = previousMeaningful(putFieldInsn.getPrevious());
    while (prev != null && prev.getOpcode() == Opcodes.CHECKCAST) {
      prev = previousMeaningful(prev.getPrevious());
    }
    return prev != null && prev.getOpcode() == Opcodes.ACONST_NULL;
  }

  @Nullable
  private static AbstractInsnNode previousMeaningful(@Nullable AbstractInsnNode insn) {
    AbstractInsnNode cur = insn;
    while (cur != null && (cur instanceof LabelNode || cur instanceof LineNumberNode || cur instanceof FrameNode)) {
      cur = cur.getPrevious();
    }
    return cur;
  }

  @Nullable
  private static AbstractInsnNode nextMeaningful(@Nullable AbstractInsnNode insn) {
    AbstractInsnNode cur = insn;
    while (cur != null && (cur instanceof LabelNode || cur instanceof LineNumberNode || cur instanceof FrameNode)) {
      cur = cur.getNext();
    }
    return cur;
  }
}
