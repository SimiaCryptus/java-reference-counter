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

import com.simiacryptus.ref.bytecode.model.BytecodeTypeIndex;
import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@RefIgnore
public final class BytecodeInstructionUtil {
  private BytecodeInstructionUtil() {
  }

  public static int getLineNumber(@Nonnull InsnList instructions, @Nullable AbstractInsnNode target) {
    if (target == null) return findFirstLine(instructions);
    AbstractInsnNode cur = target;
    while (cur != null) {
      if (cur instanceof LineNumberNode) {
        return ((LineNumberNode) cur).line;
      }
      cur = cur.getPrevious();
    }
    return findFirstLine(instructions);
  }

  public static int findFirstLine(@Nullable InsnList instructions) {
    if (instructions == null) return -1;
    for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
      if (insn instanceof LineNumberNode) {
        return ((LineNumberNode) insn).line;
      }
    }
    return -1;
  }

  public static int findFirstClassLine(@Nullable ClassNode classNode) {
    if (classNode == null || classNode.methods == null) return -1;
    int best = -1;
    for (MethodNode m : classNode.methods) {
      int line = findFirstLine(m.instructions);
      if (line > 0 && (best < 0 || line < best)) {
        best = line;
      }
    }
    return best;
  }

  @Nullable
  public static LocalVariableNode findLocalVariable(
      @Nonnull MethodNode method,
      int slot,
      @Nullable AbstractInsnNode atInsn
  ) {
    List<LocalVariableNode> locals = method.localVariables;
    if (locals == null || locals.isEmpty()) return null;
    int atIdx = atInsn != null && method.instructions.contains(atInsn)
        ? method.instructions.indexOf(atInsn)
        : -1;
    LocalVariableNode fallback = null;
    for (LocalVariableNode lv : locals) {
      if (lv.index != slot) continue;
      if (fallback == null) {
        fallback = lv;
      }
      if (atIdx >= 0 && lv.start != null && lv.end != null) {
        int startIdx = method.instructions.indexOf(lv.start);
        int endIdx = method.instructions.indexOf(lv.end);
        // Note: on ASTORE, the variable's scope label often starts at the next instruction
        if (startIdx >= 0 && endIdx >= 0 && atIdx >= startIdx - 2 && atIdx <= endIdx) {
          return lv;
        }
      }
    }
    return fallback;
  }

  @Nonnull
  public static String resolveLocalName(
      @Nonnull MethodNode method,
      int slot,
      @Nullable AbstractInsnNode atInsn
  ) {
    boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
    if (!isStatic && slot == 0) {
      return "this";
    }
    LocalVariableNode lv = findLocalVariable(method, slot, atInsn);
    if (lv != null && lv.name != null && !lv.name.isEmpty()) {
      return lv.name;
    }
    // Check MethodParameters attribute if present
    Type[] argTypes = Type.getArgumentTypes(method.desc);
    int curSlot = isStatic ? 0 : 1;
    for (int i = 0; i < argTypes.length; i++) {
      if (curSlot == slot) {
        if (method.parameters != null && i < method.parameters.size()) {
          ParameterNode pn = method.parameters.get(i);
          if (pn != null && pn.name != null && !pn.name.isEmpty()) {
            return pn.name;
          }
        }
        return "param" + i;
      }
      curSlot += argTypes[i].getSize();
    }
    return "local$" + slot;
  }

  @Nullable
  public static String resolveLocalSignature(
      @Nonnull MethodNode method,
      int slot,
      @Nullable AbstractInsnNode atInsn
  ) {
    LocalVariableNode lv = findLocalVariable(method, slot, atInsn);
    return lv != null ? lv.signature : null;
  }

  public static boolean isInstanceFreeRef(@Nonnull MethodInsnNode call) {
    if (call.getOpcode() == Opcodes.INVOKESTATIC) return false;
    return "freeRef".equals(call.name) && ("()I".equals(call.desc) || "()V".equals(call.desc));
  }

  public static boolean isStaticFreeRef(@Nonnull MethodInsnNode call) {
    if (call.getOpcode() != Opcodes.INVOKESTATIC) return false;
    if (!BytecodeTypeIndex.INTERNAL_REF_UTIL.equals(call.owner)) return false;
    return ("freeRef".equals(call.name) && "(Ljava/lang/Object;)V".equals(call.desc))
        || ("freeRefs".equals(call.name) && "([Ljava/lang/Object;)V".equals(call.desc))
        || ("isNotNull".equals(call.name) && "(Ljava/lang/Object;)Z".equals(call.desc));
  }

  public static boolean isInstanceAddRef(@Nonnull MethodInsnNode call) {
    if (call.getOpcode() == Opcodes.INVOKESTATIC) return false;
    if (!"addRef".equals(call.name)) return false;
    return Type.getArgumentTypes(call.desc).length == 0
        && Type.getReturnType(call.desc).getSort() == Type.OBJECT;
  }

  public static boolean isStaticAddRef(@Nonnull MethodInsnNode call) {
    if (call.getOpcode() != Opcodes.INVOKESTATIC) return false;
    if (!"addRef".equals(call.name)) return false;
    Type[] args = Type.getArgumentTypes(call.desc);
    Type ret = Type.getReturnType(call.desc);
    return args.length == 1 && (ret.getSort() == Type.OBJECT || ret.getSort() == Type.ARRAY);
  }

  /**
   * Returns true if this static/instance call returns its first argument unchanged without modifying refcounts
   * (e.g. Objects.requireNonNull, Preconditions.checkNotNull, Class.cast, Kotlin TypeIntrinsics).
   */
  public static boolean isIdentityPassThrough(@Nonnull MethodInsnNode call) {
    if (call.getOpcode() == Opcodes.INVOKESTATIC) {
      if ("java/util/Objects".equals(call.owner) && "requireNonNull".equals(call.name)) {
        Type[] args = Type.getArgumentTypes(call.desc);
        return args.length >= 1 && Type.getReturnType(call.desc).getSort() == Type.OBJECT;
      }
      if ("com/google/common/base/Preconditions".equals(call.owner) && "checkNotNull".equals(call.name)) {
        return true;
      }
      if ("kotlin/jvm/internal/TypeIntrinsics".equals(call.owner)
          && call.name.startsWith("beforeCheckcast")) {
        return true;
      }
    } else if ("java/lang/Class".equals(call.owner)
        && "cast".equals(call.name)
        && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(call.desc)) {
      return true;
    }
    return false;
  }

  /**
   * Returns true if this call is a Kotlin/Java null-assertion intrinsic that guarantees argument 0 is non-null
   * if it returns normally.
   */
  public static boolean assertsFirstArgNonNull(@Nonnull MethodInsnNode call) {
    if (call.getOpcode() != Opcodes.INVOKESTATIC) return false;
    if ("kotlin/jvm/internal/Intrinsics".equals(call.owner)) {
      return call.name.startsWith("checkNotNull");
    }
    if ("java/util/Objects".equals(call.owner) && "requireNonNull".equals(call.name)) {
      return true;
    }
    return "com/google/common/base/Preconditions".equals(call.owner) && "checkNotNull".equals(call.name);
  }

  /**
   * Returns true if this method inspects/borrows its arguments without taking ownership and should never
   * be flagged as a blind parameter sink (e.g., equals, Objects.*, Kotlin Intrinsics.*, assertions, logging).
   */
  public static boolean isNonConsumingIntrinsic(@Nonnull MethodInsnNode call) {
    if ("equals".equals(call.name) && "(Ljava/lang/Object;)Z".equals(call.desc)) {
      return true;
    }
    if (BytecodeTypeIndex.INTERNAL_REF_UTIL.equals(call.owner)) {
      if ("assertAlive".equals(call.name) || "watch".equals(call.name) || "addRefs".equals(call.name)) {
        return true;
      }
    }
    if (call.getOpcode() == Opcodes.INVOKESTATIC) {
      if ("kotlin/jvm/internal/Intrinsics".equals(call.owner)) {
        return true;
      }
      if ("scala/runtime/BoxesRunTime".equals(call.owner) && call.name.startsWith("equals")) {
        return true;
      }
      if ("java/util/Objects".equals(call.owner)) {
        return "equals".equals(call.name)
            || "deepEquals".equals(call.name)
            || "hashCode".equals(call.name)
            || "hash".equals(call.name)
            || "isNull".equals(call.name)
            || "nonNull".equals(call.name)
            || "toString".equals(call.name)
            || "requireNonNull".equals(call.name);
      }
      if ("java/lang/System".equals(call.owner) && "identityHashCode".equals(call.name)) {
        return true;
      }
      if ("java/lang/String".equals(call.owner) && "valueOf".equals(call.name)) {
        return true;
      }
      if (call.owner.startsWith("org/junit/")
          || call.owner.startsWith("kotlin/test/")
          || call.owner.startsWith("org/testng/")) {
        return call.name.startsWith("assert") || call.name.startsWith("check");
      }
    } else {
      if ("java/lang/StringBuilder".equals(call.owner) || "java/lang/StringBuffer".equals(call.owner)) {
        return "append".equals(call.name);
      }
      if ("java/lang/Class".equals(call.owner)) {
        return "isInstance".equals(call.name) || "cast".equals(call.name);
      }
      if ("org/slf4j/Logger".equals(call.owner) || "java/io/PrintStream".equals(call.owner)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if calling this method on an owned receiver consumes the receiver's reference
   * (e.g., Optional.get() unwraps and transfers ownership of the contained element).
   */
  public static boolean methodConsumesReceiver(@Nonnull MethodInsnNode call) {
    if (call.getOpcode() == Opcodes.INVOKESTATIC) return false;
    if (BytecodeTypeIndex.INTERNAL_OPTIONAL.equals(call.owner)) {
      return "get".equals(call.name) || "orElse".equals(call.name) || "orElseThrow".equals(call.name);
    }
    return false;
  }

  public static boolean isSyntheticMethodToSkip(@Nonnull ClassNode owner, @Nonnull MethodNode method, boolean verifyLambdas) {
    if ((method.access & Opcodes.ACC_BRIDGE) != 0) {
      return true;
    }
    if ((method.access & Opcodes.ACC_ABSTRACT) != 0 || (method.access & Opcodes.ACC_NATIVE) != 0) {
      return true;
    }
    String name = method.name;
    if ("addRef".equals(name) || "freeRefs".equals(name) || "equals".equals(name) || "hashCode".equals(name)) {
      return true;
    }
    if ((owner.access & Opcodes.ACC_ENUM) != 0 && ("values".equals(name) || "valueOf".equals(name))) {
      return true;
    }
    if (owner.name.endsWith("$WhenMappings")) {
      return true;
    }
    if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
      if (isSyntheticLambdaBody(name)) {
        return !verifyLambdas;
      }
      // Skip compiler-generated accessors (access$000), Kotlin $default stubs, Groovy callsite helpers, etc.
      return true;
    }
    // Kotlin $default methods may or may not have ACC_SYNTHETIC set on all versions; check pattern
    if (name.endsWith("$default") && method.desc.contains("ILjava/lang/Object;)")) {
      return true;
    }
    return false;
  }

  public static boolean isSyntheticLambdaBody(@Nonnull String methodName) {
    return methodName.startsWith("lambda$")
        || methodName.contains("$lambda$")
        || methodName.contains("$lambda-")
        || methodName.startsWith("$anonfun$");
  }
}
