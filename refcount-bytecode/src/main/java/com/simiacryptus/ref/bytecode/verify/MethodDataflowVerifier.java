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
import com.simiacryptus.ref.bytecode.VerifierConfig;
import com.simiacryptus.ref.bytecode.model.*;
import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Intra-procedural control-flow and abstract stack/local reference-ownership verifier for JVM bytecode.
 * Works uniformly across Java, Kotlin, Scala, Groovy, and synthetic closure methods.
 */
@RefIgnore
public final class MethodDataflowVerifier {
  private static final int MAX_OWNED_COUNT = 4;
  private static final int NO_PARENT_ID = Integer.MIN_VALUE;

  @Nonnull
  private final BytecodeTypeIndex typeIndex;
  @Nonnull
  private final VerifierConfig config;

  public MethodDataflowVerifier(@Nonnull BytecodeTypeIndex typeIndex, @Nonnull VerifierConfig config) {
    this.typeIndex = typeIndex;
    this.config = config;
  }

  public int verifyClass(@Nonnull ClassMetadata cls, @Nonnull List<Diagnostic> out) {
    if (cls.isRefIgnore() || cls.isInterface() || cls.isAnnotation() || cls.isModuleInfo() || cls.isSynthetic()) {
      return 0;
    }
    ClassNode classNode = cls.getClassNode();
    if (classNode == null) return 0;

    int analyzedMethods = 0;
    for (MethodMetadata methodMeta : cls.getMethods()) {
      MethodNode methodNode = methodMeta.getMethodNode();
      if (methodNode == null || methodMeta.isRefIgnore()) continue;
      if (BytecodeInstructionUtil.isSyntheticMethodToSkip(classNode, methodNode, config.isVerifySyntheticLambdas())) {
        continue;
      }
      if (methodNode.instructions == null || methodNode.instructions.size() == 0) {
        continue;
      }
      analyzedMethods++;
      verifyMethod(cls, methodMeta, methodNode, out);
    }
    return analyzedMethods;
  }

  private void verifyMethod(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata methodMeta,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out
  ) {
    int n = method.instructions.size();
    FrameState[] inStates = new FrameState[n];
    Deque<Integer> worklist = new ArrayDeque<>();
    Set<String> emittedDiagnosticKeys = new HashSet<>();

    FrameState entry = createInitialState(cls, methodMeta, method);
    inStates[0] = entry;
    worklist.add(0);

    // Pre-index synthetic MONITOREXIT rethrow handlers and all exception handler entry labels
    Set<Integer> syntheticMonitorHandlers = findSyntheticMonitorHandlers(method);
    Set<LabelNode> allHandlerLabels = new HashSet<>();
    if (method.tryCatchBlocks != null) {
      for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
        if (tcb.handler != null) {
          allHandlerLabels.add(tcb.handler);
        }
      }
    }

    int maxLocals = Math.max(method.maxLocals + 4, entry.locals.length);

    while (!worklist.isEmpty()) {
      int idx = worklist.pollFirst();
      FrameState state = inStates[idx];
      if (state == null) continue;

      AbstractInsnNode insn = method.instructions.get(idx);

      // Seed reachable try/catch/finally handlers at non-handler try-start boundaries
      if (insn instanceof LabelNode && method.tryCatchBlocks != null && !allHandlerLabels.contains(insn)) {
        for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
          if (tcb.start == insn && tcb.handler != null) {
            int handlerIdx = method.instructions.indexOf(tcb.handler);
            if (handlerIdx >= 0 && !syntheticMonitorHandlers.contains(handlerIdx)) {
              FrameState handlerState = state.copyForExceptionHandler(
                  tcb.type != null ? Type.getObjectType(tcb.type) : Type.getObjectType("java/lang/Throwable"),
                  tcb.type == null
              );
              propagate(handlerIdx, handlerState, inStates, worklist, cls, methodMeta, method, out, emittedDiagnosticKeys);
            }
          }
        }
      }

      FrameState next = state.copy(maxLocals);
      stepInstruction(idx, insn, next, inStates, worklist, syntheticMonitorHandlers, cls, methodMeta, method, out, emittedDiagnosticKeys);
    }
  }

  @Nonnull
  private FrameState createInitialState(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata methodMeta,
      @Nonnull MethodNode method
  ) {
    boolean isStatic = methodMeta.isStatic();
    Type[] argTypes = Type.getArgumentTypes(method.desc);
    int slotCursor = isStatic ? 0 : 1;
    for (Type t : argTypes) {
      slotCursor += t.getSize();
    }
    int localCapacity = Math.max(method.maxLocals + 2, slotCursor + 2);
    SymVal[] locals = new SymVal[localCapacity];
    Arrays.fill(locals, SymVal.UNINITIALIZED);
    Map<Integer, RefEntry> refs = new LinkedHashMap<>();

    int firstLine = BytecodeInstructionUtil.findFirstLine(method.instructions);

    if (!isStatic) {
      Type selfType = Type.getObjectType(cls.getInternalName());
      if (typeIndex.isReferenceCountingClass(cls.getInternalName()) || cls.isRefAware()) {
        int selfId = -1;
        refs.put(selfId, new RefEntry(selfId, "this", firstLine, true, 0, Nullness.NON_NULL, NO_PARENT_ID, -1));
        locals[0] = SymVal.ref(selfId, selfType, null);
      } else {
        locals[0] = SymVal.untracked(selfType, null);
      }
    }

    // Determine if this is a synthetic lambda method with leading captured outer parameters
    int capturedLambdaParams = countLeadingCapturedLambdaParams(cls, methodMeta, method, argTypes);
    List<String> paramSigs = SignatureUtil.extractMethodParameterSignatures(method.signature);
    MethodContract selfContract = typeIndex.resolveMethodContract(
        cls.getInternalName(), methodMeta.getName(), methodMeta.getDescriptor()
    );

    int slot = isStatic ? 0 : 1;
    for (int i = 0; i < argTypes.length; i++) {
      Type pType = argTypes[i];
      String pSig = i < paramSigs.size() ? paramSigs.get(i) : BytecodeInstructionUtil.resolveLocalSignature(method, slot, null);
      String pName = BytecodeInstructionUtil.resolveLocalName(method, slot, method.instructions.getFirst());

      boolean isRefType = typeIndex.isRefCountedType(pType, pSig) || selfContract.isParamRefAware(i);
      boolean isIgnored = selfContract.isParamRefIgnore(i);
      boolean isSyntheticCapture = selfContract.isParamSynthetic(i)
          || i < capturedLambdaParams
          || ("<init>".equals(method.name) && cls.isAnonymousOrClosure() && isAnonymousCapturedCtorParam(cls, i, pType));

      if (isRefType) {
        int refId = -2 - slot;
        if (isIgnored || isSyntheticCapture) {
          refs.put(refId, new RefEntry(refId, pName, firstLine, true, 0, Nullness.MAYBE_NULL, NO_PARENT_ID, -1));
        } else {
          refs.put(refId, new RefEntry(refId, pName, firstLine, false, 1, Nullness.MAYBE_NULL, NO_PARENT_ID, -1));
        }
        locals[slot] = SymVal.ref(refId, pType, pSig);
      } else {
        locals[slot] = SymVal.untracked(pType, pSig);
      }
      if (pType.getSize() == 2) {
        locals[slot + 1] = SymVal.TOP_SLOT;
      }
      slot += pType.getSize();
    }

    return new FrameState(locals, new ArrayList<>(), refs, false);
  }

  /**
   * For synthetic lambda body methods (e.g. {@code lambda$foo$0}), parameters corresponding to captured variables
   * precede the SAM interface's explicit parameters and are borrowed from the enclosing closure.
   */
  private int countLeadingCapturedLambdaParams(
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata methodMeta,
      @Nonnull MethodNode method,
      @Nonnull Type[] lambdaArgTypes
  ) {
    if (!BytecodeInstructionUtil.isSyntheticLambdaBody(method.name)) {
      return 0;
    }
    ClassNode cn = cls.getClassNode();
    if (cn == null || cn.methods == null) return 0;
    // Look for an InvokeDynamicInsnNode in the enclosing class targeting this synthetic method
    for (MethodNode m : cn.methods) {
      if (m.instructions == null) continue;
      for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
        if (insn instanceof InvokeDynamicInsnNode) {
          InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
          if (indy.bsmArgs != null) {
            for (Object bsmArg : indy.bsmArgs) {
              if (bsmArg instanceof Handle) {
                Handle h = (Handle) bsmArg;
                if (cls.getInternalName().equals(h.getOwner())
                    && method.name.equals(h.getName())
                    && method.desc.equals(h.getDesc())) {
                  Type[] capturedTypes = Type.getArgumentTypes(indy.desc);
                  int capturedSlots = capturedTypes.length;
                  // If non-static lambda$ method, capture arg 0 of indy is 'this', leaving capturedSlots - 1 method args
                  if (!methodMeta.isStatic() && capturedSlots > 0) {
                    return Math.max(0, Math.min(lambdaArgTypes.length, capturedSlots - 1));
                  }
                  return Math.min(lambdaArgTypes.length, capturedSlots);
                }
              }
            }
          }
        }
      }
    }
    return 0;
  }

  private boolean isAnonymousCapturedCtorParam(@Nonnull ClassMetadata cls, int paramIndex, @Nonnull Type paramType) {
    for (FieldMetadata f : cls.getFields()) {
      if (f.isSynthetic()
          && (f.getName().startsWith("val$") || f.getName().startsWith("$"))
          && f.getDescriptor().equals(paramType.getDescriptor())) {
        return true;
      }
    }
    return false;
  }

  private void stepInstruction(
      int idx,
      @Nonnull AbstractInsnNode insn,
      @Nonnull FrameState state,
      @Nonnull FrameState[] inStates,
      @Nonnull Deque<Integer> worklist,
      @Nonnull Set<Integer> syntheticMonitorHandlers,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata methodMeta,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    int op = insn.getOpcode();
    if (op < 0) {
      // LabelNode, LineNumberNode, FrameNode
      fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
      return;
    }

    int line = BytecodeInstructionUtil.getLineNumber(method.instructions, insn);

    switch (op) {
      case Opcodes.NOP:
      case Opcodes.IINC:
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.ACONST_NULL:
        state.push(SymVal.NULL_VAL);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.ICONST_M1:
      case Opcodes.ICONST_0:
      case Opcodes.ICONST_1:
      case Opcodes.ICONST_2:
      case Opcodes.ICONST_3:
      case Opcodes.ICONST_4:
      case Opcodes.ICONST_5:
      case Opcodes.BIPUSH:
      case Opcodes.SIPUSH:
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LCONST_0:
      case Opcodes.LCONST_1:
        state.pushWide(SymVal.untracked(Type.LONG_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.FCONST_0:
      case Opcodes.FCONST_1:
      case Opcodes.FCONST_2:
        state.push(SymVal.untracked(Type.FLOAT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.DCONST_0:
      case Opcodes.DCONST_1:
        state.pushWide(SymVal.untracked(Type.DOUBLE_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LDC: {
        Object cst = ((LdcInsnNode) insn).cst;
        if (cst instanceof Long) {
          state.pushWide(SymVal.untracked(Type.LONG_TYPE, null));
        } else if (cst instanceof Double) {
          state.pushWide(SymVal.untracked(Type.DOUBLE_TYPE, null));
        } else if (cst instanceof Float) {
          state.push(SymVal.untracked(Type.FLOAT_TYPE, null));
        } else if (cst instanceof Integer) {
          state.push(SymVal.untracked(Type.INT_TYPE, null));
        } else if (cst instanceof String) {
          state.push(SymVal.untracked(Type.getObjectType("java/lang/String"), null));
        } else if (cst instanceof Type) {
          state.push(SymVal.untracked(Type.getObjectType("java/lang/Class"), null));
        } else {
          state.push(SymVal.untracked(Type.getObjectType("java/lang/Object"), null));
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.ILOAD:
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      case Opcodes.FLOAD:
        state.push(SymVal.untracked(Type.FLOAT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      case Opcodes.LLOAD:
        state.pushWide(SymVal.untracked(Type.LONG_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      case Opcodes.DLOAD:
        state.pushWide(SymVal.untracked(Type.DOUBLE_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.ALOAD: {
        int slot = ((VarInsnNode) insn).var;
        SymVal loaded = slot < state.locals.length ? state.locals[slot] : SymVal.UNINITIALIZED;
        if (loaded == SymVal.UNINITIALIZED || loaded == SymVal.TOP_SLOT) {
          String sig = BytecodeInstructionUtil.resolveLocalSignature(method, slot, insn);
          loaded = SymVal.untracked(Type.getObjectType("java/lang/Object"), sig);
        }
        state.push(loaded);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.ISTORE:
      case Opcodes.FSTORE: {
        int slot = ((VarInsnNode) insn).var;
        state.pop1();
        state.setLocal(slot, SymVal.untracked(op == Opcodes.ISTORE ? Type.INT_TYPE : Type.FLOAT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.LSTORE:
      case Opcodes.DSTORE: {
        int slot = ((VarInsnNode) insn).var;
        state.pop2();
        state.setLocalWide(slot, SymVal.untracked(op == Opcodes.LSTORE ? Type.LONG_TYPE : Type.DOUBLE_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.ASTORE: {
        int slot = ((VarInsnNode) insn).var;
        SymVal val = state.pop1();
        String localName = BytecodeInstructionUtil.resolveLocalName(method, slot, insn);
        String localSig = BytecodeInstructionUtil.resolveLocalSignature(method, slot, insn);
        LocalVariableNode lvn = BytecodeInstructionUtil.findLocalVariable(method, slot, insn);

        // Check if overwriting an existing owned reference in this slot that has no other live alias
        SymVal prev = slot < state.locals.length ? state.locals[slot] : SymVal.UNINITIALIZED;
        state.setLocal(slot, val);

        if (val.isRef()) {
          RefEntry entry = state.refs.get(val.refId);
          if (entry != null && entry.label.startsWith("new ") || (entry != null && entry.label.contains("("))) {
            if (!localName.startsWith("local$")) {
              entry.label = localName;
            }
          }
          // Check blind local assignment (e.g. Object obj = new MyResource() without @RefAware)
          if (lvn != null && lvn.desc != null) {
            Type declaredLocalType = Type.getType(lvn.desc);
            if (BytecodeTypeIndex.INTERNAL_OBJECT.equals(declaredLocalType.getInternalName())
                && !typeIndex.isRefCountedType(declaredLocalType, localSig)
                && entry != null && !entry.borrowed && entry.ownedCount > 0
                && !localName.startsWith("temp")) {
              // Note: Kotlin/Scala temporary variables and destructuring slots may erase to Object only if untyped;
              // check only when LocalVariableTable explicitly types a user variable as java/lang/Object.
            }
          }
        }

        if (prev.isRef() && (!val.isRef() || prev.refId != val.refId)) {
          RefEntry prevEntry = state.refs.get(prev.refId);
          if (prevEntry != null && prevEntry.ownedCount > 0 && prevEntry.nullness != Nullness.IS_NULL
              && !state.isRefReachable(prev.refId)) {
            emitOnce(out, emittedKeys, new Diagnostic(
                RuleId.UNFREED_REFERENCE,
                Severity.ERROR,
                cls.getBinaryName(),
                method.name,
                method.desc,
                cls.getSourceFile(),
                line,
                cls.getLanguage(),
                cls.getOriginPath(),
                String.format(
                    "Overwriting variable '%s' leaks unfreed reference '%s' (allocated/acquired at line %d)",
                    localName,
                    prevEntry.label,
                    prevEntry.originLine
                )
            ));
            prevEntry.ownedCount = 0;
          }
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.IALOAD:
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.SALOAD:
        state.pop1();
        state.pop1();
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.FALOAD:
        state.pop1();
        state.pop1();
        state.push(SymVal.untracked(Type.FLOAT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LALOAD:
      case Opcodes.DALOAD:
        state.pop1();
        state.pop1();
        state.pushWide(SymVal.untracked(op == Opcodes.LALOAD ? Type.LONG_TYPE : Type.DOUBLE_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.AALOAD: {
        state.pop1(); // index
        SymVal arr = state.pop1();
        checkNotUseAfterFree(arr, state, line, "array access", cls, method, out, emittedKeys);
        Type elemType = (arr.type != null && arr.type.getSort() == Type.ARRAY)
            ? Type.getType(arr.type.getDescriptor().substring(1))
            : Type.getObjectType("java/lang/Object");
        if (typeIndex.isRefCountedType(elemType, null)) {
          int elemRefId = idx;
          state.refs.put(elemRefId, new RefEntry(elemRefId, "arrayElement", line, true, 0, Nullness.MAYBE_NULL, NO_PARENT_ID, -1));
          state.push(SymVal.ref(elemRefId, elemType, null));
        } else {
          state.push(SymVal.untracked(elemType, null));
        }
        checkUnreachableOwnedRefs(arr, state, line, "array element access on temporary array", cls, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.IASTORE:
      case Opcodes.BASTORE:
      case Opcodes.CASTORE:
      case Opcodes.SASTORE:
      case Opcodes.FASTORE:
        state.pop1();
        state.pop1();
        state.pop1();
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LASTORE:
      case Opcodes.DASTORE:
        state.pop2();
        state.pop1();
        state.pop1();
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.AASTORE: {
        SymVal elem = state.pop1();
        state.pop1(); // index
        SymVal arr = state.pop1();
        checkNotUseAfterFree(arr, state, line, "array store", cls, method, out, emittedKeys);
        if (elem.isRef()) {
          // Storing a reference-counted element into an array transfers 1 reference into the array
          consumeOwnedRef(elem, state, line, "array element store", cls, method, out, emittedKeys);
          // If the target array was an Object[] (e.g. varargs array for wrapInterface), promote it to tracked array!
          if (!arr.isRef() && arr.untrackedOriginIdx >= 0) {
            int arrRefId = arr.untrackedOriginIdx;
            if (!state.refs.containsKey(arrRefId)) {
              state.refs.put(arrRefId, new RefEntry(
                  arrRefId, "refArray (line " + line + ")", line, false, 1, Nullness.NON_NULL, NO_PARENT_ID, -1
              ));
            }
            state.upgradeUntrackedOriginToRef(arr.untrackedOriginIdx, arrRefId);
          }
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.POP: {
        SymVal popped = state.pop1();
        handlePoppedValue(popped, state, line, cls, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.POP2: {
        SymVal top = state.peek();
        if (top == SymVal.TOP_SLOT) {
          state.pop2();
        } else {
          SymVal v1 = state.pop1();
          SymVal v2 = state.pop1();
          handlePoppedValue(v1, state, line, cls, method, out, emittedKeys);
          handlePoppedValue(v2, state, line, cls, method, out, emittedKeys);
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.DUP: {
        SymVal v = state.pop1();
        state.push(v);
        state.push(v);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.DUP_X1: {
        SymVal v1 = state.pop1();
        SymVal v2 = state.pop1();
        state.push(v1);
        state.push(v2);
        state.push(v1);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.DUP_X2: {
        SymVal v1 = state.pop1();
        SymVal v2 = state.pop1();
        SymVal v3 = state.pop1();
        state.push(v1);
        state.push(v3);
        state.push(v2);
        state.push(v1);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.DUP2: {
        SymVal v1 = state.pop1();
        SymVal v2 = state.pop1();
        state.push(v2);
        state.push(v1);
        state.push(v2);
        state.push(v1);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.DUP2_X1: {
        SymVal v1 = state.pop1();
        SymVal v2 = state.pop1();
        SymVal v3 = state.pop1();
        state.push(v2);
        state.push(v1);
        state.push(v3);
        state.push(v2);
        state.push(v1);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.DUP2_X2: {
        SymVal v1 = state.pop1();
        SymVal v2 = state.pop1();
        SymVal v3 = state.pop1();
        SymVal v4 = state.pop1();
        state.push(v2);
        state.push(v1);
        state.push(v4);
        state.push(v3);
        state.push(v2);
        state.push(v1);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.SWAP: {
        SymVal v1 = state.pop1();
        SymVal v2 = state.pop1();
        state.push(v1);
        state.push(v2);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      // Arithmetic, bitwise, and primitive conversion opcodes
      case Opcodes.IADD:
      case Opcodes.ISUB:
      case Opcodes.IMUL:
      case Opcodes.IDIV:
      case Opcodes.IREM:
      case Opcodes.IAND:
      case Opcodes.IOR:
      case Opcodes.IXOR:
      case Opcodes.ISHL:
      case Opcodes.ISHR:
      case Opcodes.IUSHR:
      case Opcodes.FADD:
      case Opcodes.FSUB:
      case Opcodes.FMUL:
      case Opcodes.FDIV:
      case Opcodes.FREM:
      case Opcodes.FCMPL:
      case Opcodes.FCMPG:
        state.pop1();
        state.pop1();
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LADD:
      case Opcodes.LSUB:
      case Opcodes.LMUL:
      case Opcodes.LDIV:
      case Opcodes.LREM:
      case Opcodes.LAND:
      case Opcodes.LOR:
      case Opcodes.LXOR:
      case Opcodes.DADD:
      case Opcodes.DSUB:
      case Opcodes.DMUL:
      case Opcodes.DDIV:
      case Opcodes.DREM:
        state.pop2();
        state.pop2();
        state.pushWide(SymVal.untracked(Type.LONG_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LSHL:
      case Opcodes.LSHR:
      case Opcodes.LUSHR:
        state.pop1();
        state.pop2();
        state.pushWide(SymVal.untracked(Type.LONG_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LCMP:
      case Opcodes.DCMPL:
      case Opcodes.DCMPG:
        state.pop2();
        state.pop2();
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.INEG:
      case Opcodes.FNEG:
      case Opcodes.I2F:
      case Opcodes.F2I:
      case Opcodes.I2B:
      case Opcodes.I2C:
      case Opcodes.I2S:
        state.pop1();
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.LNEG:
      case Opcodes.DNEG:
      case Opcodes.L2D:
      case Opcodes.D2L:
        state.pop2();
        state.pushWide(SymVal.untracked(Type.LONG_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.I2L:
      case Opcodes.I2D:
      case Opcodes.F2L:
      case Opcodes.F2D:
        state.pop1();
        state.pushWide(SymVal.untracked(Type.LONG_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.L2I:
      case Opcodes.L2F:
      case Opcodes.D2I:
      case Opcodes.D2F:
        state.pop2();
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.ARRAYLENGTH: {
        SymVal arr = state.pop1();
        checkNotUseAfterFree(arr, state, line, "arraylength", cls, method, out, emittedKeys);
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        checkUnreachableOwnedRefs(arr, state, line, "arraylength on temporary array", cls, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.MONITORENTER:
      case Opcodes.MONITOREXIT: {
        SymVal lock = state.pop1();
        if (op == Opcodes.MONITORENTER) {
          checkNotUseAfterFree(lock, state, line, "synchronized monitor", cls, method, out, emittedKeys);
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.NEW: {
        Type objType = Type.getObjectType(((TypeInsnNode) insn).desc);
        // Push uninitialized allocation marker carrying the NEW instruction index
        state.push(SymVal.uninitializedNew(idx, objType));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.NEWARRAY:
        state.pop1();
        state.push(SymVal.untracked(Type.getType("[I"), null));
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;

      case Opcodes.ANEWARRAY: {
        state.pop1(); // length
        Type elemType = Type.getObjectType(((TypeInsnNode) insn).desc);
        Type arrType = Type.getType("[" + elemType.getDescriptor());
        if (typeIndex.isRefCountedType(arrType, null)) {
          state.refs.put(idx, new RefEntry(
              idx, "new " + elemType.getClassName() + "[] (line " + line + ")", line, false, 1, Nullness.NON_NULL, NO_PARENT_ID, -1
          ));
          state.push(SymVal.ref(idx, arrType, null));
        } else {
          state.push(SymVal.untrackedWithOrigin(arrType, null, idx));
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.MULTIANEWARRAY: {
        MultiANewArrayInsnNode multi = (MultiANewArrayInsnNode) insn;
        for (int d = 0; d < multi.dims; d++) {
          state.pop1();
        }
        Type arrType = Type.getType(multi.desc);
        if (typeIndex.isRefCountedType(arrType, null)) {
          state.refs.put(idx, new RefEntry(
              idx, "new " + arrType.getClassName() + " (line " + line + ")", line, false, 1, Nullness.NON_NULL, NO_PARENT_ID, -1
          ));
          state.push(SymVal.ref(idx, arrType, null));
        } else {
          state.push(SymVal.untracked(arrType, null));
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.CHECKCAST: {
        Type castType = Type.getObjectType(((TypeInsnNode) insn).desc);
        SymVal val = state.pop1();
        if (val.isNull()) {
          state.push(SymVal.NULL_VAL);
        } else if (val.isRef()) {
          state.push(SymVal.ref(val.refId, castType, val.genericSignature));
        } else if (typeIndex.isRefCountedType(castType, null)) {
          // If an @RefAware generic method returned Object and is immediately cast to a ReferenceCounting type!
          if (val.fromRefAwareReturn) {
            state.refs.put(idx, new RefEntry(
                idx, "cast<" + castType.getClassName() + "> (line " + line + ")", line, false, 1, Nullness.MAYBE_NULL, NO_PARENT_ID, -1
            ));
            state.push(SymVal.ref(idx, castType, null));
          } else {
            // Borrowed cast from generic Object
            state.refs.put(idx, new RefEntry(
                idx, "cast<" + castType.getClassName() + "> (line " + line + ")", line, true, 0, Nullness.MAYBE_NULL, NO_PARENT_ID, -1
            ));
            state.push(SymVal.ref(idx, castType, null));
          }
        } else {
          state.push(SymVal.untracked(castType, val.genericSignature));
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.INSTANCEOF: {
        SymVal val = state.pop1();
        checkNotUseAfterFree(val, state, line, "instanceof check", cls, method, out, emittedKeys);
        state.push(SymVal.untracked(Type.INT_TYPE, null));
        checkUnreachableOwnedRefs(val, state, line, "instanceof on temporary reference", cls, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.GETSTATIC: {
        FieldInsnNode fInsn = (FieldInsnNode) insn;
        Type fType = Type.getType(fInsn.desc);
        String fSig = resolveFieldSignature(fInsn.owner, fInsn.name);
        if (fType.getSize() == 2) {
          state.pushWide(SymVal.untracked(fType, fSig));
        } else if (typeIndex.isRefCountedType(fType, fSig) || isFieldRefAware(fInsn.owner, fInsn.name)) {
          state.refs.put(idx, new RefEntry(idx, fInsn.name, line, true, 0, Nullness.MAYBE_NULL, NO_PARENT_ID, -1));
          state.push(SymVal.ref(idx, fType, fSig));
        } else {
          state.push(SymVal.untracked(fType, fSig));
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.PUTSTATIC: {
        FieldInsnNode fInsn = (FieldInsnNode) insn;
        Type fType = Type.getType(fInsn.desc);
        SymVal val = fType.getSize() == 2 ? state.pop2() : state.pop1();
        if (val.isRef()) {
          consumeOwnedRef(val, state, line, "static field '" + fInsn.name + "'", cls, method, out, emittedKeys);
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.GETFIELD: {
        FieldInsnNode fInsn = (FieldInsnNode) insn;
        SymVal ownerVal = state.pop1();
        checkNotUseAfterFree(ownerVal, state, line, "field access '" + fInsn.name + "'", cls, method, out, emittedKeys);
        Type fType = Type.getType(fInsn.desc);
        String fSig = resolveFieldSignature(fInsn.owner, fInsn.name);
        if (fType.getSize() == 2) {
          state.pushWide(SymVal.untracked(fType, fSig));
        } else if (typeIndex.isRefCountedType(fType, fSig) || isFieldRefAware(fInsn.owner, fInsn.name)) {
          state.refs.put(idx, new RefEntry(idx, "this." + fInsn.name, line, true, 0, Nullness.MAYBE_NULL, NO_PARENT_ID, -1));
          state.push(SymVal.ref(idx, fType, fSig));
        } else {
          state.push(SymVal.untracked(fType, fSig));
        }
        checkUnreachableOwnedRefs(ownerVal, state, line, "field access '" + fInsn.name + "' on temporary reference", cls, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.PUTFIELD: {
        FieldInsnNode fInsn = (FieldInsnNode) insn;
        Type fType = Type.getType(fInsn.desc);
        SymVal val = fType.getSize() == 2 ? state.pop2() : state.pop1();
        SymVal ownerVal = state.pop1();
        checkNotUseAfterFree(ownerVal, state, line, "field write '" + fInsn.name + "'", cls, method, out, emittedKeys);

        String fSig = resolveFieldSignature(fInsn.owner, fInsn.name);
        boolean fieldIgnored = isFieldRefIgnore(fInsn.owner, fInsn.name);
        boolean fieldRefAware = isFieldRefAware(fInsn.owner, fInsn.name) || typeIndex.isRefCountedType(fType, fSig);
        boolean syntheticAnonCaptureInit = "<init>".equals(method.name)
            && cls.isAnonymousOrClosure()
            && isFieldSynthetic(fInsn.owner, fInsn.name);

        if (val.isRef() && !fieldIgnored && !syntheticAnonCaptureInit) {
          if (!fieldRefAware && config.isStrictBlindParameters()) {
            emitOnce(out, emittedKeys, new Diagnostic(
                RuleId.BLIND_PARAMETER_PASS,
                Severity.ERROR,
                cls.getBinaryName(),
                method.name,
                method.desc,
                cls.getSourceFile(),
                line,
                cls.getLanguage(),
                cls.getOriginPath(),
                String.format(
                    "Assigning reference-counted value to non-@RefAware field '%s' of type '%s'",
                    fInsn.name,
                    fType.getClassName()
                )
            ));
          }
          consumeOwnedRef(val, state, line, "field assignment '" + fInsn.name + "'", cls, method, out, emittedKeys);
        }
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.INVOKEVIRTUAL:
      case Opcodes.INVOKESPECIAL:
      case Opcodes.INVOKESTATIC:
      case Opcodes.INVOKEINTERFACE: {
        handleMethodInvocation(idx, (MethodInsnNode) insn, state, line, cls, methodMeta, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.INVOKEDYNAMIC: {
        handleInvokeDynamic(idx, (InvokeDynamicInsnNode) insn, state, line, cls, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.GOTO: {
        int targetIdx = method.instructions.indexOf(((JumpInsnNode) insn).label);
        propagate(targetIdx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.IFNULL:
      case Opcodes.IFNONNULL: {
        SymVal cond = state.pop1();
        int jumpTarget = method.instructions.indexOf(((JumpInsnNode) insn).label);
        if (cond.isNull()) {
          if (op == Opcodes.IFNULL) {
            propagate(jumpTarget, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
          } else {
            fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
          }
          return;
        }
        if (cond.isRef()) {
          RefEntry entry = state.refs.get(cond.refId);
          if (entry != null) {
            if (entry.nullness == Nullness.NON_NULL) {
              if (op == Opcodes.IFNONNULL) {
                propagate(jumpTarget, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              } else {
                fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              }
              return;
            }
            if (entry.nullness == Nullness.IS_NULL) {
              if (op == Opcodes.IFNULL) {
                propagate(jumpTarget, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              } else {
                fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              }
              return;
            }
            FrameState nullBranch = state.copy(state.locals.length);
            nullBranch.markRefIsNull(cond.refId);
            FrameState nonNullBranch = state.copy(state.locals.length);
            nonNullBranch.markRefNonNull(cond.refId);

            if (op == Opcodes.IFNULL) {
              propagate(jumpTarget, nullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              fallThrough(idx, nonNullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
            } else {
              propagate(jumpTarget, nonNullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              fallThrough(idx, nullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
            }
            return;
          }
        }
        FrameState branched = state.copy(state.locals.length);
        propagate(jumpTarget, branched, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.IFEQ:
      case Opcodes.IFNE:
      case Opcodes.IFLT:
      case Opcodes.IFGE:
      case Opcodes.IFGT:
      case Opcodes.IFLE: {
        state.pop1();
        int jumpTarget = method.instructions.indexOf(((JumpInsnNode) insn).label);
        FrameState branched = state.copy(state.locals.length);
        propagate(jumpTarget, branched, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.IF_ICMPEQ:
      case Opcodes.IF_ICMPNE:
      case Opcodes.IF_ICMPLT:
      case Opcodes.IF_ICMPGE:
      case Opcodes.IF_ICMPGT:
      case Opcodes.IF_ICMPLE:
      case Opcodes.IF_ACMPEQ:
      case Opcodes.IF_ACMPNE: {
        SymVal v2 = state.pop1();
        SymVal v1 = state.pop1();
        int jumpTarget = method.instructions.indexOf(((JumpInsnNode) insn).label);
        // Handle null == x / x == null idioms emitted as IF_ACMPEQ / IF_ACMPNE by some compilers
        if ((op == Opcodes.IF_ACMPEQ || op == Opcodes.IF_ACMPNE) && (v1.isNull() ^ v2.isNull())) {
          SymVal refSide = v1.isNull() ? v2 : v1;
          if (refSide.isRef()) {
            RefEntry entry = state.refs.get(refSide.refId);
            if (entry != null) {
              if (entry.nullness == Nullness.NON_NULL) {
                if (op == Opcodes.IF_ACMPNE) {
                  propagate(jumpTarget, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
                } else {
                  fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
                }
                return;
              }
              if (entry.nullness == Nullness.IS_NULL) {
                if (op == Opcodes.IF_ACMPEQ) {
                  propagate(jumpTarget, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
                } else {
                  fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
                }
                return;
              }
              FrameState nullBranch = state.copy(state.locals.length);
              nullBranch.markRefIsNull(refSide.refId);
              FrameState nonNullBranch = state.copy(state.locals.length);
              nonNullBranch.markRefNonNull(refSide.refId);
              if (op == Opcodes.IF_ACMPEQ) {
                propagate(jumpTarget, nullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
                fallThrough(idx, nonNullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              } else {
                propagate(jumpTarget, nonNullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
                fallThrough(idx, nullBranch, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
              }
              return;
            }
          }
        }
        FrameState branched = state.copy(state.locals.length);
        propagate(jumpTarget, branched, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        return;
      }

      case Opcodes.TABLESWITCH: {
        state.pop1();
        TableSwitchInsnNode ts = (TableSwitchInsnNode) insn;
        if (ts.dflt != null) {
          propagate(method.instructions.indexOf(ts.dflt), state.copy(state.locals.length),
              inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        }
        if (ts.labels != null) {
          for (LabelNode lbl : ts.labels) {
            propagate(method.instructions.indexOf(lbl), state.copy(state.locals.length),
                inStates, worklist, cls, methodMeta, method, out, emittedKeys);
          }
        }
        return;
      }

      case Opcodes.LOOKUPSWITCH: {
        state.pop1();
        LookupSwitchInsnNode ls = (LookupSwitchInsnNode) insn;
        if (ls.dflt != null) {
          propagate(method.instructions.indexOf(ls.dflt), state.copy(state.locals.length),
              inStates, worklist, cls, methodMeta, method, out, emittedKeys);
        }
        if (ls.labels != null) {
          for (LabelNode lbl : ls.labels) {
            propagate(method.instructions.indexOf(lbl), state.copy(state.locals.length),
                inStates, worklist, cls, methodMeta, method, out, emittedKeys);
          }
        }
        return;
      }

      case Opcodes.RETURN:
      case Opcodes.IRETURN:
      case Opcodes.FRETURN: {
        if (op != Opcodes.RETURN) state.pop1();
        verifyAllOwnedRefsConsumedAtExit(state, line, false, cls, method, out, emittedKeys);
        return;
      }

      case Opcodes.LRETURN:
      case Opcodes.DRETURN: {
        state.pop2();
        verifyAllOwnedRefsConsumedAtExit(state, line, false, cls, method, out, emittedKeys);
        return;
      }

      case Opcodes.ARETURN: {
        SymVal retVal = state.pop1();
        handleReturnValue(retVal, state, line, cls, methodMeta, method, out, emittedKeys);
        verifyAllOwnedRefsConsumedAtExit(state, line, false, cls, method, out, emittedKeys);
        return;
      }

      case Opcodes.ATHROW: {
        SymVal thrown = state.pop1();
        // Route explicit throw to any enclosing TryCatchBlockNode handler first
        List<TryCatchBlockNode> enclosingHandlers = findEnclosingHandlers(method, idx);
        if (!enclosingHandlers.isEmpty()) {
          for (TryCatchBlockNode tcb : enclosingHandlers) {
            int handlerIdx = method.instructions.indexOf(tcb.handler);
            if (handlerIdx >= 0 && handlerIdx != idx && !syntheticMonitorHandlers.contains(handlerIdx)) {
              FrameState handlerState = state.copyForExceptionHandler(
                  thrown.type != null ? thrown.type : Type.getObjectType("java/lang/Throwable"),
                  tcb.type == null
              );
              propagate(handlerIdx, handlerState, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
            }
          }
          return;
        }
        // Uncaught throw exiting the method: verify owned references are freed (unless inside a synthetic monitor handler)
        if (!state.inCatchAllFinally || hasAnyFreeInMethod(method)) {
          verifyAllOwnedRefsConsumedAtExit(state, line, true, cls, method, out, emittedKeys);
        }
        return;
      }

      default:
        fallThrough(idx, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
    }
  }

  private void handleMethodInvocation(
      int idx,
      @Nonnull MethodInsnNode call,
      @Nonnull FrameState state,
      int line,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata enclosingMethodMeta,
      @Nonnull MethodNode enclosingMethod,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    boolean isStatic = call.getOpcode() == Opcodes.INVOKESTATIC;
    Type[] argTypes = Type.getArgumentTypes(call.desc);
    Type retType = Type.getReturnType(call.desc);

    SymVal[] args = new SymVal[argTypes.length];
    for (int i = argTypes.length - 1; i >= 0; i--) {
      args[i] = argTypes[i].getSize() == 2 ? state.pop2() : state.pop1();
    }
    SymVal receiver = isStatic ? null : state.pop1();

    // 1. Constructor <init> invocation on NEW allocation or this/super
    if ("<init>".equals(call.name)) {
      MethodContract ctorContract = typeIndex.resolveMethodContract(call.owner, call.name, call.desc);
      ClassMetadata ctorOwnerMeta = typeIndex.resolveClass(call.owner);
      boolean isAnonClosureCtor = ctorOwnerMeta != null && ctorOwnerMeta.isAnonymousOrClosure();

      for (int i = 0; i < args.length; i++) {
        SymVal arg = args[i];
        if (!arg.isRef()) continue;
        checkNotUseAfterFree(arg, state, line, "constructor argument " + i + " of " + call.owner, cls, enclosingMethod, out, emittedKeys);
        if (ctorContract.isMethodRefIgnore() || ctorContract.isParamRefIgnore(i)) {
          continue;
        }
        if (ctorContract.isParamSynthetic(i) || (isAnonClosureCtor && isAnonymousCapturedCtorParam(ctorOwnerMeta, i, argTypes[i]))) {
          // Synthetic captured closure arguments are borrowed at construction site
          continue;
        }
        boolean paramIsRef = typeIndex.isRefCountedType(argTypes[i], null) || ctorContract.isParamRefAware(i);
        if (paramIsRef) {
          consumeOwnedRef(arg, state, line, "constructor argument " + i + " of " + simpleName(call.owner), cls, enclosingMethod, out, emittedKeys);
        } else if (config.isStrictBlindParameters()) {
          emitOnce(out, emittedKeys, new Diagnostic(
              RuleId.BLIND_PARAMETER_PASS,
              Severity.ERROR,
              cls.getBinaryName(),
              enclosingMethod.name,
              enclosingMethod.desc,
              cls.getSourceFile(),
              line,
              cls.getLanguage(),
              cls.getOriginPath(),
              String.format(
                  "Reference-counted value passed to non-@RefAware constructor parameter %d of '%s'",
                  i,
                  call.owner.replace('/', '.')
              )
          ));
        }
      }

      // Upgrade uninitializedNew tokens on stack/locals into an initialized value!
      if (receiver != null && receiver.uninitializedNewIdx >= 0) {
        int allocIdx = receiver.uninitializedNewIdx;
        Type allocType = Type.getObjectType(call.owner);
        int allocLine = BytecodeInstructionUtil.getLineNumber(enclosingMethod.instructions, enclosingMethod.instructions.get(allocIdx));
        if (typeIndex.isRefCountedType(allocType, null)) {
          state.refs.put(allocIdx, new RefEntry(
              allocIdx, "new " + simpleName(call.owner) + " (line " + allocLine + ")", allocLine, false, 1, Nullness.NON_NULL, NO_PARENT_ID, -1
          ));
          state.replaceUninitializedNew(allocIdx, SymVal.ref(allocIdx, allocType, null));
        } else {
          state.replaceUninitializedNew(allocIdx, SymVal.untracked(allocType, null));
        }
      }
      return;
    }

    // 2. Special handling for freeRef / RefUtil.freeRef / RefUtil.freeRefs / RefUtil.isNotNull
    if (BytecodeInstructionUtil.isInstanceFreeRef(call) && receiver != null) {
      freeTargetReference(receiver, state, line, cls, enclosingMethod, out, emittedKeys);
      if (retType.getSort() != Type.VOID) {
        state.push(SymVal.untracked(retType, null));
      }
      return;
    }
    if (BytecodeInstructionUtil.isStaticFreeRef(call) && args.length == 1) {
      freeTargetReference(args[0], state, line, cls, enclosingMethod, out, emittedKeys);
      if (retType.getSort() != Type.VOID) {
        state.push(SymVal.untracked(retType, null));
      }
      return;
    }

    // 3. Special handling for instance/static addRef()
    if (BytecodeInstructionUtil.isInstanceAddRef(call) && receiver != null) {
      SymVal added = incrementReference(idx, receiver, retType, state, line, cls, enclosingMethod, out, emittedKeys);
      state.push(added);
      return;
    }
    if (BytecodeInstructionUtil.isStaticAddRef(call) && args.length == 1) {
      SymVal added = incrementReference(idx, args[0], retType, state, line, cls, enclosingMethod, out, emittedKeys);
      state.push(added);
      return;
    }

    // 4. Special handling for detach() on ReferenceCounting (returns same value without altering ownedCount)
    if (!isStatic && "detach".equals(call.name) && args.length == 0 && receiver != null
        && typeIndex.isReferenceCountingClass(call.owner)) {
      checkNotUseAfterFree(receiver, state, line, "detach()", cls, enclosingMethod, out, emittedKeys);
      state.push(receiver);
      return;
    }

    // 5. Identity pass-throughs (Objects.requireNonNull, Preconditions.checkNotNull, Class.cast, etc.)
    if (BytecodeInstructionUtil.isIdentityPassThrough(call)) {
      SymVal target = isStatic ? (args.length > 0 ? args[0] : SymVal.NULL_VAL) : (args.length > 0 ? args[0] : receiver);
      if (target != null) {
        checkNotUseAfterFree(target, state, line, call.name, cls, enclosingMethod, out, emittedKeys);
        if (BytecodeInstructionUtil.assertsFirstArgNonNull(call) && target.isRef()) {
          state.markRefNonNull(target.refId);
        }
        state.push(target);
      } else {
        state.push(SymVal.untracked(retType, null));
      }
      return;
    }

    // 6. Kotlin/Java non-null assertions (e.g. Intrinsics.checkNotNullParameter / checkNotNull)
    if (BytecodeInstructionUtil.assertsFirstArgNonNull(call) && args.length >= 1) {
      if (args[0].isRef()) {
        checkNotUseAfterFree(args[0], state, line, call.name, cls, enclosingMethod, out, emittedKeys);
        state.markRefNonNull(args[0].refId);
      }
      if (retType.getSort() != Type.VOID) {
        state.push(args[0]);
      }
      return;
    }

    // 7. Standard method call: validate receiver & arguments
    if (receiver != null) {
      checkNotUseAfterFree(receiver, state, line, "method call '" + call.name + "'", cls, enclosingMethod, out, emittedKeys);
      if (BytecodeInstructionUtil.methodConsumesReceiver(call) && receiver.isRef()) {
        consumeOwnedRef(receiver, state, line, call.name + "()", cls, enclosingMethod, out, emittedKeys);
      }
    }

    MethodContract contract = typeIndex.resolveMethodContract(call.owner, call.name, call.desc);
    boolean intrinsicNonConsuming = BytecodeInstructionUtil.isNonConsumingIntrinsic(call);

    MethodMetadata targetMethodMeta = findTargetMethodMetadata(call.owner, call.name, call.desc);
    List<String> targetParamSigs = targetMethodMeta != null
        ? SignatureUtil.extractMethodParameterSignatures(targetMethodMeta.getSignature())
        : Collections.emptyList();

    for (int i = 0; i < args.length; i++) {
      SymVal arg = args[i];
      if (!arg.isRef()) continue;
      checkNotUseAfterFree(arg, state, line, "argument " + i + " of '" + call.name + "'", cls, enclosingMethod, out, emittedKeys);
      if (intrinsicNonConsuming || contract.isParamRefIgnore(i)) {
        continue;
      }
      if (contract.isMethodRefIgnore() && !contract.isParamRefAware(i) && BytecodeTypeIndex.INTERNAL_REF_UTIL.equals(call.owner)) {
        continue;
      }
      String pSig = i < targetParamSigs.size() ? targetParamSigs.get(i) : null;
      boolean paramRefCounted = typeIndex.isRefCountedType(argTypes[i], pSig);
      boolean paramRefAware = contract.isParamRefAware(i) || isImplicitConsumingJdkFactory(call, i);

      if (contract.isMethodRefIgnore() && !paramRefAware) {
        continue;
      }
      if (paramRefCounted || paramRefAware) {
        consumeOwnedRef(arg, state, line, "argument " + i + " of '" + call.name + "'", cls, enclosingMethod, out, emittedKeys);
      } else if (config.isStrictBlindParameters()) {
        emitOnce(out, emittedKeys, new Diagnostic(
            RuleId.BLIND_PARAMETER_PASS,
            Severity.ERROR,
            cls.getBinaryName(),
            enclosingMethod.name,
            enclosingMethod.desc,
            cls.getSourceFile(),
            line,
            cls.getLanguage(),
            cls.getOriginPath(),
            String.format(
                "Reference-counted value passed to non-@RefAware ('blind') parameter %d of '%s.%s'",
                i,
                simpleName(call.owner),
                call.name
            )
        ));
      }
    }

    // 8. Push return value onto stack
    if (retType.getSort() != Type.VOID) {
      if (retType.getSize() == 2) {
        state.pushWide(SymVal.untracked(retType, null));
      } else {
        String retSig = targetMethodMeta != null
            ? SignatureUtil.extractMethodReturnSignature(targetMethodMeta.getSignature())
            : null;
        boolean retIsConcreteRef = typeIndex.isRefCountedType(retType, retSig);
        boolean retIsRefAware = contract.isReturnRefAware() && !BytecodeTypeIndex.INTERNAL_REF_UTIL.equals(call.owner);
        if (BytecodeTypeIndex.INTERNAL_REF_UTIL.equals(call.owner) && !contract.isMethodRefIgnore() && contract.isReturnRefAware()) {
          retIsRefAware = true;
        }
        // Check if generic receiver signature proves the element type is NOT reference-counted (e.g. RefList<String>.get(0))
        boolean genericReceiverNonRef = false;
        if (!retIsConcreteRef && retIsRefAware && receiver != null && receiver.genericSignature != null) {
          List<Type> receiverTypeArgs = SignatureUtil.extractTopLevelTypeArguments(receiver.genericSignature);
          if (!receiverTypeArgs.isEmpty()) {
            boolean anyArgRef = false;
            for (Type ta : receiverTypeArgs) {
              if (typeIndex.isRefCountedType(ta, null)) {
                anyArgRef = true;
                break;
              }
            }
            if (!anyArgRef) {
              genericReceiverNonRef = true;
            }
          }
        }

        // Also inspect following CHECKCAST if generic return type is erased to Object
        AbstractInsnNode nextMeaningful = nextMeaningful(enclosingMethod.instructions.get(idx).getNext());
        boolean nextCastIsNonRef = false;
        if (!retIsConcreteRef && retIsRefAware && BytecodeTypeIndex.INTERNAL_OBJECT.equals(retType.getInternalName())
            && nextMeaningful instanceof TypeInsnNode && nextMeaningful.getOpcode() == Opcodes.CHECKCAST) {
          Type castTarget = Type.getObjectType(((TypeInsnNode) nextMeaningful).desc);
          if (!typeIndex.isRefCountedType(castTarget, null) && !BytecodeTypeIndex.INTERNAL_OBJECT.equals(castTarget.getInternalName())) {
            nextCastIsNonRef = true;
          }
        }

        if ((retIsConcreteRef && !contract.isMethodRefIgnore())
            || (retIsRefAware && !genericReceiverNonRef && !nextCastIsNonRef && retType.getSort() == Type.OBJECT)) {
          // If the method is @RefIgnore (e.g. peek() or getInner()), its return value is BORROWED, not newly owned!
          state.refs.put(idx, new RefEntry(
              idx, call.name + "(...) (line " + line + ")", line, false, 1, Nullness.MAYBE_NULL, NO_PARENT_ID, -1
          ));
          state.push(SymVal.ref(idx, retType, retSig));
        } else if (retIsConcreteRef && contract.isMethodRefIgnore()) {
          state.refs.put(idx, new RefEntry(
              idx, call.name + "(...) (line " + line + ")", line, true, 0, Nullness.MAYBE_NULL, NO_PARENT_ID, -1
          ));
          state.push(SymVal.ref(idx, retType, retSig));
        } else {
          state.push(SymVal.untrackedWithRefAwareHint(retType, retSig, retIsRefAware));
        }
      }
    }

    // 9. If receiver was a temporary owned reference (e.g. makeResource().doWork()), report chained temporary leak immediately!
    if (receiver != null) {
      checkUnreachableOwnedRefs(
          receiver, state, line,
          "chained method call '" + call.name + "()' on temporary reference",
          cls, enclosingMethod, out, emittedKeys
      );
    }
  }

  private void handleInvokeDynamic(
      int idx,
      @Nonnull InvokeDynamicInsnNode indy,
      @Nonnull FrameState state,
      int line,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode enclosingMethod,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    Type[] capturedTypes = Type.getArgumentTypes(indy.desc);
    Type retType = Type.getReturnType(indy.desc);
    for (int i = capturedTypes.length - 1; i >= 0; i--) {
      SymVal arg = capturedTypes[i].getSize() == 2 ? state.pop2() : state.pop1();
      if (arg.isRef()) {
        checkNotUseAfterFree(arg, state, line, "lambda capture", cls, enclosingMethod, out, emittedKeys);
        // Note: LambdaMetafactory borrows captured variables without incrementing or consuming their refCount.
        // If the outer variable is returned/leaked without freeRef() (or without RefUtil.wrapInterface), exit check catches it!
      }
    }
    if (retType.getSort() != Type.VOID) {
      if (retType.getSize() == 2) {
        state.pushWide(SymVal.untracked(retType, null));
      } else if (typeIndex.isRefCountedType(retType, null)) {
        state.refs.put(idx, new RefEntry(
            idx, "lambda (line " + line + ")", line, false, 1, Nullness.NON_NULL, NO_PARENT_ID, -1
        ));
        state.push(SymVal.ref(idx, retType, null));
      } else {
        state.push(SymVal.untracked(retType, null));
      }
    }
  }

  private void handleReturnValue(
      @Nonnull SymVal retVal,
      @Nonnull FrameState state,
      int line,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata methodMeta,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (retVal.isNull() || !retVal.isRef()) {
      return;
    }
    checkNotUseAfterFree(retVal, state, line, "return statement", cls, method, out, emittedKeys);

    Type declaredRetType = Type.getReturnType(method.desc);
    String retSig = SignatureUtil.extractMethodReturnSignature(method.signature);
    MethodContract selfContract = typeIndex.resolveMethodContract(cls.getInternalName(), method.name, method.desc);

    boolean methodReturnsRef = typeIndex.isRefCountedType(declaredRetType, retSig)
        || selfContract.isReturnRefAware()
        || (retSig != null && retSig.startsWith("T")); // generic type variable return T

    if (!methodReturnsRef && !methodMeta.isRefIgnore()) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.RETURN_LOSES_REF_AWARENESS,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Returning reference-counted value as '%s' loses reference-counting awareness (annotate method with @RefAware or return a ReferenceCounting type)",
              declaredRetType.getClassName()
          )
      ));
    }

    consumeOwnedRef(retVal, state, line, "return from '" + method.name + "'", cls, method, out, emittedKeys);
  }

  private void handlePoppedValue(
      @Nonnull SymVal popped,
      @Nonnull FrameState state,
      int line,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (!popped.isRef()) return;
    RefEntry entry = state.refs.get(popped.refId);
    if (entry == null || entry.ownedCount <= 0 || entry.nullness == Nullness.IS_NULL) {
      return;
    }
    // Statement-style r.addRef(); pops the returned alias while r is still held in a local/stack slot:
    // transfer the +1 owned reference count onto the parent RefEntry!
    if (entry.parentRefId != NO_PARENT_ID) {
      RefEntry parent = state.refs.get(entry.parentRefId);
      if (parent != null) {
        parent.ownedCount = Math.min(MAX_OWNED_COUNT, parent.ownedCount + entry.ownedCount);
        parent.nullness = Nullness.NON_NULL;
        entry.ownedCount = 0;
        if (state.isRefReachable(parent.id)) {
          return;
        }
      }
    }
    // If no other local or stack slot holds this refId, popping it immediately leaks an owned reference!
    if (!state.isRefReachable(popped.refId)) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.DISCARDED_REFERENCE,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Owned reference '%s' is discarded without calling freeRef()",
              entry.label
          )
      ));
      entry.ownedCount = 0;
    }
  }

  private void freeTargetReference(
      @Nonnull SymVal target,
      @Nonnull FrameState state,
      int line,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (target.isNull()) return;
    if (!target.isRef()) return;
    RefEntry entry = state.refs.get(target.refId);
    if (entry == null || entry.nullness == Nullness.IS_NULL) return;

    // Allow freeing this.field inside _free() or before reassigning field (FieldLifecycleVerifier validates field rules)
    if (entry.borrowed && entry.label.startsWith("this.")) {
      entry.consumedAtLine = line;
      return;
    }

    if (entry.borrowed && entry.ownedCount == 0) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.UNOWNED_REFERENCE_CONSUMED,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Cannot call freeRef() on borrowed/unowned reference '%s'",
              entry.label
          )
      ));
      return;
    }

    if (entry.ownedCount <= 0) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.DOUBLE_FREE,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Double-free / over-release of reference '%s' (previously consumed at line %d)",
              entry.label,
              entry.consumedAtLine
          )
      ));
      return;
    }

    entry.ownedCount--;
    if (entry.ownedCount == 0) {
      entry.consumedAtLine = line;
    }
  }

  @Nonnull
  private SymVal incrementReference(
      int insnIdx,
      @Nonnull SymVal target,
      @Nonnull Type retType,
      @Nonnull FrameState state,
      int line,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (target.isNull()) {
      return SymVal.NULL_VAL;
    }
    checkNotUseAfterFree(target, state, line, "addRef()", cls, method, out, emittedKeys);
    int parentId = target.isRef() ? target.refId : NO_PARENT_ID;
    String baseLabel = target.isRef() && state.refs.containsKey(target.refId)
        ? state.refs.get(target.refId).label + ".addRef()"
        : "addRef() (line " + line + ")";
    Nullness resNullness = BytecodeTypeIndex.INTERNAL_REF_UTIL.equals(retType.getInternalName())
        ? Nullness.MAYBE_NULL
        : (target.isRef() && state.refs.containsKey(target.refId) ? state.refs.get(target.refId).nullness : Nullness.NON_NULL);
    state.refs.put(insnIdx, new RefEntry(
        insnIdx, baseLabel, line, false, 1, resNullness, parentId, -1
    ));
    Type effectiveType = target.type != null && !BytecodeTypeIndex.INTERNAL_OBJECT.equals(target.type.getInternalName())
        ? target.type
        : retType;
    return SymVal.ref(insnIdx, effectiveType, target.genericSignature);
  }

  private void consumeOwnedRef(
      @Nonnull SymVal val,
      @Nonnull FrameState state,
      int line,
      @Nonnull String sinkDescription,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (val.isNull() || !val.isRef()) return;
    RefEntry entry = state.refs.get(val.refId);
    if (entry == null || entry.nullness == Nullness.IS_NULL) return;

    if (entry.borrowed && entry.ownedCount == 0) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.UNOWNED_REFERENCE_CONSUMED,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Borrowed/unowned reference '%s' transferred to %s without calling addRef()",
              entry.label,
              sinkDescription
          )
      ));
      return;
    }

    if (entry.ownedCount <= 0) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.USE_AFTER_FREE,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Reference '%s' already consumed at line %d cannot be transferred to %s",
              entry.label,
              entry.consumedAtLine,
              sinkDescription
          )
      ));
      return;
    }

    entry.ownedCount--;
    if (entry.ownedCount == 0) {
      entry.consumedAtLine = line;
    }
  }

  private void checkNotUseAfterFree(
      @Nonnull SymVal val,
      @Nonnull FrameState state,
      int line,
      @Nonnull String action,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (!val.isRef()) return;
    RefEntry entry = state.refs.get(val.refId);
    if (entry == null || entry.borrowed || entry.nullness == Nullness.IS_NULL) return;
    if (entry.ownedCount <= 0 && entry.consumedAtLine >= 0) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.USE_AFTER_FREE,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Use-after-free of reference '%s' in %s (previously freed/consumed at line %d)",
              entry.label,
              action,
              entry.consumedAtLine
          )
      ));
    }
  }

  private void checkUnreachableOwnedRefs(
      @Nonnull SymVal candidate,
      @Nonnull FrameState state,
      int line,
      @Nonnull String context,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (!candidate.isRef()) return;
    RefEntry entry = state.refs.get(candidate.refId);
    if (entry != null && entry.ownedCount > 0 && entry.nullness != Nullness.IS_NULL
        && !state.isRefReachable(candidate.refId)) {
      emitOnce(out, emittedKeys, new Diagnostic(
          RuleId.DISCARDED_REFERENCE,
          Severity.ERROR,
          cls.getBinaryName(),
          method.name,
          method.desc,
          cls.getSourceFile(),
          line,
          cls.getLanguage(),
          cls.getOriginPath(),
          String.format(
              "Temporary owned reference '%s' leaked during %s without calling freeRef()",
              entry.label,
              context
          )
      ));
      entry.ownedCount = 0;
    }
  }

  private void verifyAllOwnedRefsConsumedAtExit(
      @Nonnull FrameState state,
      int exitLine,
      boolean exceptionalExit,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    for (RefEntry entry : state.refs.values()) {
      if (entry.ownedCount > 0 && entry.nullness != Nullness.IS_NULL) {
        emitOnce(out, emittedKeys, new Diagnostic(
            RuleId.UNFREED_REFERENCE,
            Severity.ERROR,
            cls.getBinaryName(),
            method.name,
            method.desc,
            cls.getSourceFile(),
            exitLine > 0 ? exitLine : entry.originLine,
            cls.getLanguage(),
            cls.getOriginPath(),
            String.format(
                "Reference '%s' (acquired at line %d, remaining refs=%d) is not freed before %s of '%s'",
                entry.label,
                entry.originLine,
                entry.ownedCount,
                exceptionalExit ? "throw" : "return",
                method.name
            )
        ));
      }
    }
  }

  private void fallThrough(
      int currentIdx,
      @Nonnull FrameState state,
      @Nonnull FrameState[] inStates,
      @Nonnull Deque<Integer> worklist,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata methodMeta,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (currentIdx + 1 < inStates.length) {
      propagate(currentIdx + 1, state, inStates, worklist, cls, methodMeta, method, out, emittedKeys);
    }
  }

  private void propagate(
      int targetIdx,
      @Nonnull FrameState incoming,
      @Nonnull FrameState[] inStates,
      @Nonnull Deque<Integer> worklist,
      @Nonnull ClassMetadata cls,
      @Nonnull MethodMetadata methodMeta,
      @Nonnull MethodNode method,
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> emittedKeys
  ) {
    if (targetIdx < 0 || targetIdx >= inStates.length) return;
    FrameState existing = inStates[targetIdx];
    if (existing == null) {
      inStates[targetIdx] = incoming.copy(incoming.locals.length);
      worklist.addLast(targetIdx);
      return;
    }
    int line = BytecodeInstructionUtil.getLineNumber(method.instructions, method.instructions.get(targetIdx));
    boolean changed = existing.mergeFrom(incoming, targetIdx, line, cls, method, out, emittedKeys);
    if (changed) {
      worklist.addLast(targetIdx);
    }
  }

  private boolean isImplicitConsumingJdkFactory(@Nonnull MethodInsnNode call, int argIndex) {
    if (call.getOpcode() == Opcodes.INVOKESTATIC) {
      if (BytecodeTypeIndex.INTERNAL_OPTIONAL.equals(call.owner)
          && ("of".equals(call.name) || "ofNullable".equals(call.name))) {
        return true;
      }
      if ("java/util/Map".equals(call.owner) && "entry".equals(call.name)) {
        return true;
      }
    } else if ("<init>".equals(call.name) && call.owner.startsWith("java/util/AbstractMap$Simple")) {
      return true;
    }
    return false;
  }

  @Nullable
  private MethodMetadata findTargetMethodMetadata(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
    ClassMetadata cls = typeIndex.resolveClass(owner);
    if (cls != null) {
      MethodMetadata direct = cls.findMethod(name, desc);
      if (direct != null) return direct;
    }
    List<MethodMetadata> supers = typeIndex.findSuperMethods(owner, name, desc);
    return supers.isEmpty() ? null : supers.get(0);
  }

  @Nullable
  private String resolveFieldSignature(@Nonnull String owner, @Nonnull String fieldName) {
    ClassMetadata meta = typeIndex.resolveClass(owner);
    while (meta != null) {
      FieldMetadata f = meta.findField(fieldName);
      if (f != null) return f.getSignature();
      meta = meta.getSuperName() != null ? typeIndex.resolveClass(meta.getSuperName()) : null;
    }
    return null;
  }

  private boolean isFieldRefAware(@Nonnull String owner, @Nonnull String fieldName) {
    ClassMetadata meta = typeIndex.resolveClass(owner);
    while (meta != null) {
      FieldMetadata f = meta.findField(fieldName);
      if (f != null) return f.isRefAware();
      meta = meta.getSuperName() != null ? typeIndex.resolveClass(meta.getSuperName()) : null;
    }
    return false;
  }

  private boolean isFieldRefIgnore(@Nonnull String owner, @Nonnull String fieldName) {
    ClassMetadata meta = typeIndex.resolveClass(owner);
    while (meta != null) {
      FieldMetadata f = meta.findField(fieldName);
      if (f != null) return f.isRefIgnore();
      meta = meta.getSuperName() != null ? typeIndex.resolveClass(meta.getSuperName()) : null;
    }
    return false;
  }

  private boolean isFieldSynthetic(@Nonnull String owner, @Nonnull String fieldName) {
    ClassMetadata meta = typeIndex.resolveClass(owner);
    if (meta == null) return false;
    FieldMetadata f = meta.findField(fieldName);
    return f != null && f.isSynthetic();
  }

  @Nonnull
  private static Set<Integer> findSyntheticMonitorHandlers(@Nonnull MethodNode method) {
    Set<Integer> result = new HashSet<>();
    if (method.tryCatchBlocks == null) return result;
    for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
      if (tcb.type != null || tcb.handler == null) continue;
      int hIdx = method.instructions.indexOf(tcb.handler);
      if (hIdx < 0) continue;
      AbstractInsnNode cur = tcb.handler;
      int steps = 0;
      boolean sawMonitorExit = false;
      boolean sawFree = false;
      while (cur != null && steps < 12) {
        if (cur.getOpcode() == Opcodes.MONITOREXIT) {
          sawMonitorExit = true;
        } else if (cur instanceof MethodInsnNode) {
          MethodInsnNode m = (MethodInsnNode) cur;
          if (BytecodeInstructionUtil.isInstanceFreeRef(m) || BytecodeInstructionUtil.isStaticFreeRef(m)) {
            sawFree = true;
          }
        } else if (cur.getOpcode() == Opcodes.ATHROW) {
          break;
        }
        if (!(cur instanceof LabelNode || cur instanceof LineNumberNode || cur instanceof FrameNode)) {
          steps++;
        }
        cur = cur.getNext();
      }
      if (sawMonitorExit && !sawFree) {
        result.add(hIdx);
      }
    }
    return result;
  }

  @Nonnull
  private static List<TryCatchBlockNode> findEnclosingHandlers(@Nonnull MethodNode method, int insnIdx) {
    if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty()) {
      return Collections.emptyList();
    }
    List<TryCatchBlockNode> matches = new ArrayList<>();
    for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
      int startIdx = method.instructions.indexOf(tcb.start);
      int endIdx = method.instructions.indexOf(tcb.end);
      int handlerIdx = method.instructions.indexOf(tcb.handler);
      if (insnIdx >= startIdx && insnIdx < endIdx && insnIdx != handlerIdx) {
        matches.add(tcb);
        if (tcb.type == null) {
          // Innermost catch-all finally intercepts all Throwables
          break;
        }
      }
    }
    return matches;
  }

  private static boolean hasAnyFreeInMethod(@Nonnull MethodNode method) {
    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
      if (insn instanceof MethodInsnNode) {
        MethodInsnNode m = (MethodInsnNode) insn;
        if (BytecodeInstructionUtil.isInstanceFreeRef(m) || BytecodeInstructionUtil.isStaticFreeRef(m)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  private static AbstractInsnNode nextMeaningful(@Nullable AbstractInsnNode insn) {
    AbstractInsnNode cur = insn;
    while (cur != null && (cur instanceof LabelNode || cur instanceof LineNumberNode || cur instanceof FrameNode)) {
      cur = cur.getNext();
    }
    return cur;
  }

  @Nonnull
  private static String simpleName(@Nonnull String internalName) {
    int slash = internalName.lastIndexOf('/');
    return slash >= 0 ? internalName.substring(slash + 1) : internalName;
  }

  private static void emitOnce(
      @Nonnull List<Diagnostic> out,
      @Nonnull Set<String> keys,
      @Nonnull Diagnostic diagnostic
  ) {
    String key = diagnostic.getRuleId() + ":" + diagnostic.getLineNumber() + ":" + diagnostic.getMessage();
    if (keys.add(key)) {
      out.add(diagnostic);
    }
  }

  private enum Nullness {
    NON_NULL,
    MAYBE_NULL,
    IS_NULL
  }

  private static final class RefEntry {
    final int id;
    String label;
    final int originLine;
    boolean borrowed;
    int ownedCount;
    Nullness nullness;
    final int parentRefId;
    int consumedAtLine;
    boolean mismatchReported;

    RefEntry(
        int id,
        String label,
        int originLine,
        boolean borrowed,
        int ownedCount,
        Nullness nullness,
        int parentRefId,
        int consumedAtLine
    ) {
      this.id = id;
      this.label = label;
      this.originLine = originLine;
      this.borrowed = borrowed;
      this.ownedCount = ownedCount;
      this.nullness = nullness;
      this.parentRefId = parentRefId;
      this.consumedAtLine = consumedAtLine;
    }

    RefEntry copy() {
      RefEntry c = new RefEntry(id, label, originLine, borrowed, ownedCount, nullness, parentRefId, consumedAtLine);
      c.mismatchReported = this.mismatchReported;
      return c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RefEntry)) return false;
      RefEntry that = (RefEntry) o;
      return id == that.id
          && borrowed == that.borrowed
          && ownedCount == that.ownedCount
          && nullness == that.nullness;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, borrowed, ownedCount, nullness);
    }
  }

  private static final class SymVal {
    static final SymVal UNINITIALIZED = new SymVal(Kind.UNINITIALIZED, -1, null, null, -1, -1, false);
    static final SymVal TOP_SLOT = new SymVal(Kind.TOP_SLOT, -1, null, null, -1, -1, false);
    static final SymVal NULL_VAL = new SymVal(Kind.NULL_VAL, -1, null, null, -1, -1, false);

    enum Kind {
      UNINITIALIZED,
      TOP_SLOT,
      NULL_VAL,
      UNINITIALIZED_NEW,
      UNTRACKED,
      REF
    }

    final Kind kind;
    final int refId;
    @Nullable
    final Type type;
    @Nullable
    final String genericSignature;
    final int uninitializedNewIdx;
    final int untrackedOriginIdx;
    final boolean fromRefAwareReturn;

    private SymVal(
        Kind kind,
        int refId,
        @Nullable Type type,
        @Nullable String genericSignature,
        int uninitializedNewIdx,
        int untrackedOriginIdx,
        boolean fromRefAwareReturn
    ) {
      this.kind = kind;
      this.refId = refId;
      this.type = type;
      this.genericSignature = genericSignature;
      this.uninitializedNewIdx = uninitializedNewIdx;
      this.untrackedOriginIdx = untrackedOriginIdx;
      this.fromRefAwareReturn = fromRefAwareReturn;
    }

    static SymVal untracked(@Nullable Type type, @Nullable String sig) {
      return new SymVal(Kind.UNTRACKED, -1, type, sig, -1, -1, false);
    }

    static SymVal untrackedWithOrigin(@Nullable Type type, @Nullable String sig, int originIdx) {
      return new SymVal(Kind.UNTRACKED, -1, type, sig, -1, originIdx, false);
    }

    static SymVal untrackedWithRefAwareHint(@Nullable Type type, @Nullable String sig, boolean refAware) {
      return new SymVal(Kind.UNTRACKED, -1, type, sig, -1, -1, refAware);
    }

    static SymVal uninitializedNew(int newInsnIdx, @Nonnull Type type) {
      return new SymVal(Kind.UNINITIALIZED_NEW, -1, type, null, newInsnIdx, -1, false);
    }

    static SymVal ref(int refId, @Nullable Type type, @Nullable String sig) {
      return new SymVal(Kind.REF, refId, type, sig, -1, -1, false);
    }

    boolean isRef() {
      return kind == Kind.REF;
    }

    boolean isNull() {
      return kind == Kind.NULL_VAL;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SymVal)) return false;
      SymVal symVal = (SymVal) o;
      return kind == symVal.kind
          && refId == symVal.refId
          && uninitializedNewIdx == symVal.uninitializedNewIdx
          && untrackedOriginIdx == symVal.untrackedOriginIdx
          && fromRefAwareReturn == symVal.fromRefAwareReturn
          && Objects.equals(type, symVal.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(kind, refId, type, uninitializedNewIdx, untrackedOriginIdx, fromRefAwareReturn);
    }
  }

  private static final class FrameState {
    SymVal[] locals;
    final List<SymVal> stack;
    final Map<Integer, RefEntry> refs;
    boolean inCatchAllFinally;

    FrameState(SymVal[] locals, List<SymVal> stack, Map<Integer, RefEntry> refs, boolean inCatchAllFinally) {
      this.locals = locals;
      this.stack = stack;
      this.refs = refs;
      this.inCatchAllFinally = inCatchAllFinally;
    }

    FrameState copy(int minLocals) {
      int len = Math.max(minLocals, locals.length);
      SymVal[] newLocals = new SymVal[len];
      Arrays.fill(newLocals, SymVal.UNINITIALIZED);
      System.arraycopy(locals, 0, newLocals, 0, locals.length);
      List<SymVal> newStack = new ArrayList<>(stack);
      Map<Integer, RefEntry> newRefs = new LinkedHashMap<>(refs.size());
      for (Map.Entry<Integer, RefEntry> e : refs.entrySet()) {
        newRefs.put(e.getKey(), e.getValue().copy());
      }
      return new FrameState(newLocals, newStack, newRefs, inCatchAllFinally);
    }

    FrameState copyForExceptionHandler(@Nonnull Type exceptionType, boolean catchAll) {
      FrameState c = copy(locals.length);
      c.stack.clear();
      c.stack.add(SymVal.untracked(exceptionType, null));
      if (catchAll) {
        c.inCatchAllFinally = true;
      }
      return c;
    }

    void push(@Nonnull SymVal val) {
      stack.add(val);
    }

    void pushWide(@Nonnull SymVal val) {
      stack.add(val);
      stack.add(SymVal.TOP_SLOT);
    }

    @Nonnull
    SymVal peek() {
      return stack.isEmpty() ? SymVal.UNINITIALIZED : stack.get(stack.size() - 1);
    }

    @Nonnull
    SymVal pop1() {
      if (stack.isEmpty()) return SymVal.UNINITIALIZED;
      return stack.remove(stack.size() - 1);
    }

    @Nonnull
    SymVal pop2() {
      SymVal top = pop1();
      if (top == SymVal.TOP_SLOT) {
        return pop1();
      }
      return top;
    }

    void setLocal(int slot, @Nonnull SymVal val) {
      ensureCapacity(slot + 1);
      locals[slot] = val;
    }

    void setLocalWide(int slot, @Nonnull SymVal val) {
      ensureCapacity(slot + 2);
      locals[slot] = val;
      locals[slot + 1] = SymVal.TOP_SLOT;
    }

    private void ensureCapacity(int needed) {
      if (needed <= locals.length) return;
      SymVal[] grown = new SymVal[needed + 2];
      Arrays.fill(grown, SymVal.UNINITIALIZED);
      System.arraycopy(locals, 0, grown, 0, locals.length);
      this.locals = grown;
    }

    void replaceUninitializedNew(int newIdx, @Nonnull SymVal initialized) {
      for (int i = 0; i < locals.length; i++) {
        if (locals[i].uninitializedNewIdx == newIdx) {
          locals[i] = initialized;
        }
      }
      for (int i = 0; i < stack.size(); i++) {
        if (stack.get(i).uninitializedNewIdx == newIdx) {
          stack.set(i, initialized);
        }
      }
    }

    void upgradeUntrackedOriginToRef(int originIdx, int refId) {
      for (int i = 0; i < locals.length; i++) {
        SymVal v = locals[i];
        if (v.untrackedOriginIdx == originIdx) {
          locals[i] = SymVal.ref(refId, v.type, v.genericSignature);
        }
      }
      for (int i = 0; i < stack.size(); i++) {
        SymVal v = stack.get(i);
        if (v.untrackedOriginIdx == originIdx) {
          stack.set(i, SymVal.ref(refId, v.type, v.genericSignature));
        }
      }
    }

    boolean isRefReachable(int refId) {
      for (SymVal v : locals) {
        if (v != null && v.isRef() && v.refId == refId) return true;
      }
      for (SymVal v : stack) {
        if (v != null && v.isRef() && v.refId == refId) return true;
      }
      return false;
    }

    void markRefIsNull(int refId) {
      RefEntry entry = refs.get(refId);
      if (entry != null) {
        entry.nullness = Nullness.IS_NULL;
        entry.ownedCount = 0;
      }
      for (int i = 0; i < locals.length; i++) {
        if (locals[i].isRef() && locals[i].refId == refId) {
          locals[i] = SymVal.NULL_VAL;
        }
      }
      for (int i = 0; i < stack.size(); i++) {
        if (stack.get(i).isRef() && stack.get(i).refId == refId) {
          stack.set(i, SymVal.NULL_VAL);
        }
      }
    }

    void markRefNonNull(int refId) {
      RefEntry entry = refs.get(refId);
      if (entry != null && entry.nullness != Nullness.IS_NULL) {
        entry.nullness = Nullness.NON_NULL;
      }
    }

    boolean mergeFrom(
        @Nonnull FrameState other,
        int joinIdx,
        int line,
        @Nonnull ClassMetadata cls,
        @Nonnull MethodNode method,
        @Nonnull List<Diagnostic> out,
        @Nonnull Set<String> emittedKeys
    ) {
      boolean changed = false;
      if (other.locals.length > this.locals.length) {
        ensureCapacity(other.locals.length);
      }

      Set<Integer> absorbedThisIds = new HashSet<>();
      Set<Integer> absorbedOtherIds = new HashSet<>();

      // 1. Merge stack slots (handle ternary expressions like cond ? a : b or cond ? null : x.addRef())
      int minStack = Math.min(this.stack.size(), other.stack.size());
      for (int i = 0; i < minStack; i++) {
        SymVal a = this.stack.get(i);
        SymVal b = other.stack.get(i);
        SymVal merged = mergeSymVal(
            a, b, 100_000 + joinIdx * 16 + i, other, absorbedThisIds, absorbedOtherIds, line, cls, method, out, emittedKeys
        );
        if (!Objects.equals(a, merged)) {
          this.stack.set(i, merged);
          changed = true;
        }
      }

      // 2. Merge local slots
      for (int i = 0; i < Math.min(this.locals.length, other.locals.length); i++) {
        SymVal a = this.locals[i];
        SymVal b = other.locals[i];
        SymVal merged = mergeSymVal(
            a, b, 200_000 + joinIdx * 32 + i, other, absorbedThisIds, absorbedOtherIds, line, cls, method, out, emittedKeys
        );
        if (!Objects.equals(a, merged)) {
          this.locals[i] = merged;
          changed = true;
        }
      }

      // 3. Merge RefEntry tables
      Set<Integer> allIds = new LinkedHashSet<>(this.refs.keySet());
      allIds.addAll(other.refs.keySet());
      for (Integer id : allIds) {
        if (absorbedThisIds.contains(id) || absorbedOtherIds.contains(id)) {
          RefEntry cur = this.refs.get(id);
          if (cur != null && cur.ownedCount != 0 && !isRefReachable(id)) {
            cur.ownedCount = 0;
            changed = true;
          }
          continue;
        }
        RefEntry e1 = this.refs.get(id);
        RefEntry e2 = other.refs.get(id);
        if (e1 == null && e2 != null) {
          RefEntry copy = e2.copy();
          // Allocated only on 'other' branch -> conditional allocation (MAYBE_NULL)
          if (copy.nullness == Nullness.NON_NULL) {
            copy.nullness = Nullness.MAYBE_NULL;
          }
          this.refs.put(id, copy);
          changed = true;
        } else if (e1 != null && e2 == null) {
          if (e1.nullness == Nullness.NON_NULL) {
            e1.nullness = Nullness.MAYBE_NULL;
            changed = true;
          }
        } else if (e1 != null) {
          // Both branches know about 'id'
          Nullness mergedNull = mergeNullness(e1.nullness, e2.nullness);
          // Check branch consumption mismatch when neither branch proved the reference is NULL!
          if (e1.ownedCount != e2.ownedCount
              && e1.nullness != Nullness.IS_NULL
              && e2.nullness != Nullness.IS_NULL
              && !e1.mismatchReported && !e2.mismatchReported) {
            emitOnce(out, emittedKeys, new Diagnostic(
                RuleId.BRANCH_CONSUMPTION_MISMATCH,
                Severity.ERROR,
                cls.getBinaryName(),
                method.name,
                method.desc,
                cls.getSourceFile(),
                line,
                cls.getLanguage(),
                cls.getOriginPath(),
                String.format(
                    "Inconsistent reference consumption for '%s' across control-flow branches (%d vs %d remaining refs)",
                    e1.label,
                    e1.ownedCount,
                    e2.ownedCount
                )
            ));
            e1.mismatchReported = true;
          }
          int mergedCount;
          if (e1.nullness == Nullness.IS_NULL) {
            mergedCount = e2.ownedCount;
          } else if (e2.nullness == Nullness.IS_NULL) {
            mergedCount = e1.ownedCount;
          } else {
            mergedCount = Math.min(e1.ownedCount, e2.ownedCount);
          }
          boolean mergedBorrowed = e1.borrowed || e2.borrowed;
          if (e1.nullness != mergedNull || e1.ownedCount != mergedCount || e1.borrowed != mergedBorrowed) {
            e1.nullness = mergedNull;
            e1.ownedCount = mergedCount;
            e1.borrowed = mergedBorrowed;
            if (e1.consumedAtLine < 0 && e2.consumedAtLine >= 0) {
              e1.consumedAtLine = e2.consumedAtLine;
            }
            changed = true;
          }
        }
      }

      if (this.inCatchAllFinally != (this.inCatchAllFinally && other.inCatchAllFinally)) {
        this.inCatchAllFinally = false;
        changed = true;
      }
      return changed;
    }

    @Nonnull
    private SymVal mergeSymVal(
        @Nonnull SymVal a,
        @Nonnull SymVal b,
        int syntheticPhiId,
        @Nonnull FrameState otherState,
        @Nonnull Set<Integer> absorbedThisIds,
        @Nonnull Set<Integer> absorbedOtherIds,
        int line,
        @Nonnull ClassMetadata cls,
        @Nonnull MethodNode method,
        @Nonnull List<Diagnostic> out,
        @Nonnull Set<String> emittedKeys
    ) {
      if (Objects.equals(a, b)) return a;
      if (a == SymVal.UNINITIALIZED || b == SymVal.UNINITIALIZED) return SymVal.UNINITIALIZED;
      if (a == SymVal.TOP_SLOT || b == SymVal.TOP_SLOT) return SymVal.TOP_SLOT;

      if (a.isNull() && b.isRef()) {
        RefEntry be = otherState.refs.get(b.refId);
        if (be != null && !this.refs.containsKey(b.refId)) {
          RefEntry copy = be.copy();
          copy.nullness = Nullness.MAYBE_NULL;
          this.refs.put(b.refId, copy);
        } else if (this.refs.containsKey(b.refId)) {
          RefEntry cur = this.refs.get(b.refId);
          if (cur.nullness == Nullness.IS_NULL && be != null) {
            cur.ownedCount = be.ownedCount;
          }
          cur.nullness = Nullness.MAYBE_NULL;
        }
        return b;
      }
      if (b.isNull() && a.isRef()) {
        RefEntry ae = this.refs.get(a.refId);
        if (ae != null) {
          ae.nullness = Nullness.MAYBE_NULL;
        }
        return a;
      }

      // Merging two distinct refIds into a canonical representative (e.g. x = cond ? new MyRef() : otherRef.addRef())
      if (a.isRef() && b.isRef() && a.refId != b.refId) {
        RefEntry ae = this.refs.get(a.refId);
        RefEntry be = otherState.refs.get(b.refId);
        if (ae != null && be != null && !otherState.refs.containsKey(a.refId)) {
          if (ae.ownedCount != be.ownedCount
              && ae.nullness != Nullness.IS_NULL
              && be.nullness != Nullness.IS_NULL
              && !ae.mismatchReported) {
            emitOnce(out, emittedKeys, new Diagnostic(
                RuleId.BRANCH_CONSUMPTION_MISMATCH,
                Severity.ERROR,
                cls.getBinaryName(),
                method.name,
                method.desc,
                cls.getSourceFile(),
                line,
                cls.getLanguage(),
                cls.getOriginPath(),
                String.format(
                    "Branch merge combines references '%s' (owned=%d) and '%s' (owned=%d) with mismatched ownership",
                    ae.label, ae.ownedCount, be.label, be.ownedCount
                )
            ));
            ae.mismatchReported = true;
          }
          ae.ownedCount = Math.max(ae.ownedCount, be.ownedCount);
          ae.borrowed = ae.borrowed || be.borrowed;
          ae.nullness = mergeNullness(ae.nullness, be.nullness);
          absorbedOtherIds.add(b.refId);
          return a;
        }
      }

      if (a.isNull()) return b;
      if (b.isNull()) return a;
      return a;
    }

    private boolean isRefReachableExcept(int targetRefId, @Nonnull SymVal ignoreInstance) {
      int count = 0;
      for (SymVal v : locals) {
        if (v != null && v.isRef() && v.refId == targetRefId) count++;
      }
      for (SymVal v : stack) {
        if (v != null && v.isRef() && v.refId == targetRefId) count++;
      }
      return count > 1;
    }

    @Nonnull
    private static Nullness mergeNullness(@Nonnull Nullness n1, @Nonnull Nullness n2) {
      if (n1 == n2) return n1;
      return Nullness.MAYBE_NULL;
    }
  }
}
