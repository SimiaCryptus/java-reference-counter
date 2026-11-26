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

package com.simiacryptus.ref.bytecode.model;

import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RefIgnore
public final class MethodMetadata {
  @Nonnull
  private final String ownerInternalName;
  @Nonnull
  private final String name;
  @Nonnull
  private final String descriptor;
  @Nullable
  private final String signature;
  private final int access;
  private final boolean refAware;
  private final boolean refIgnore;
  private final boolean mustCall;
  @Nonnull
  private final boolean[] paramRefAware;
  @Nonnull
  private final boolean[] paramRefIgnore;
  @Nonnull
  private final boolean[] paramSynthetic;
  @Nullable
  private final MethodNode methodNode;

  public MethodMetadata(
      @Nonnull String ownerInternalName,
      @Nonnull String name,
      @Nonnull String descriptor,
      @Nullable String signature,
      int access,
      boolean refAware,
      boolean refIgnore,
      boolean mustCall,
      @Nonnull boolean[] paramRefAware,
      @Nonnull boolean[] paramRefIgnore,
      @Nonnull boolean[] paramSynthetic,
      @Nullable MethodNode methodNode
  ) {
    this.ownerInternalName = ownerInternalName;
    this.name = name;
    this.descriptor = descriptor;
    this.signature = signature;
    this.access = access;
    this.refAware = refAware;
    this.refIgnore = refIgnore;
    this.mustCall = mustCall;
    this.paramRefAware = paramRefAware;
    this.paramRefIgnore = paramRefIgnore;
    this.paramSynthetic = paramSynthetic;
    this.methodNode = methodNode;
  }

  @Nonnull
  public static MethodMetadata fromAsm(
      @Nonnull String ownerInternalName,
      @Nonnull MethodNode method,
      boolean nonStaticInnerClass,
      boolean includeNode
  ) {
    Type[] argTypes = Type.getArgumentTypes(method.desc);
    int count = argTypes.length;
    boolean[] pAware = new boolean[count];
    boolean[] pIgnore = new boolean[count];
    boolean[] pSynth = new boolean[count];
    boolean nonStaticInnerCtor = nonStaticInnerClass && "<init>".equals(method.name);
    for (int i = 0; i < count; i++) {
      pAware[i] = AnnotationUtil.hasParameterAnnotation(method, i, count, AnnotationUtil.DESC_REF_AWARE);
      pIgnore[i] = AnnotationUtil.hasParameterAnnotation(method, i, count, AnnotationUtil.DESC_REF_IGNORE);
      pSynth[i] = AnnotationUtil.isSyntheticParameter(method, i, count, nonStaticInnerCtor);
    }
    boolean refAware = AnnotationUtil.hasAnnotation(
        method.visibleAnnotations, method.invisibleAnnotations, AnnotationUtil.DESC_REF_AWARE
    );
    boolean refIgnore = AnnotationUtil.hasAnnotation(
        method.visibleAnnotations, method.invisibleAnnotations, AnnotationUtil.DESC_REF_IGNORE
    );
    boolean mustCall = AnnotationUtil.hasAnnotation(
        method.visibleAnnotations, method.invisibleAnnotations, AnnotationUtil.DESC_MUST_CALL
    );
    return new MethodMetadata(
        ownerInternalName,
        method.name,
        method.desc,
        method.signature,
        method.access,
        refAware,
        refIgnore,
        mustCall,
        pAware,
        pIgnore,
        pSynth,
        includeNode ? method : null
    );
  }

  @Nonnull
  public String getOwnerInternalName() {
    return ownerInternalName;
  }

  @Nonnull
  public String getName() {
    return name;
  }

  @Nonnull
  public String getDescriptor() {
    return descriptor;
  }

  @Nullable
  public String getSignature() {
    return signature;
  }

  public int getAccess() {
    return access;
  }

  public boolean isRefAware() {
    return refAware;
  }

  public boolean isRefIgnore() {
    return refIgnore;
  }

  public boolean isMustCall() {
    return mustCall;
  }

  public int getParameterCount() {
    return paramRefAware.length;
  }

  public boolean isParameterRefAware(int index) {
    return index >= 0 && index < paramRefAware.length && paramRefAware[index];
  }

  public boolean isParameterRefIgnore(int index) {
    return index >= 0 && index < paramRefIgnore.length && paramRefIgnore[index];
  }

  public boolean isParameterSynthetic(int index) {
    return index >= 0 && index < paramSynthetic.length && paramSynthetic[index];
  }

  public boolean isStatic() {
    return (access & Opcodes.ACC_STATIC) != 0;
  }

  public boolean isAbstract() {
    return (access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public boolean isNative() {
    return (access & Opcodes.ACC_NATIVE) != 0;
  }

  public boolean isBridge() {
    return (access & Opcodes.ACC_BRIDGE) != 0;
  }

  public boolean isSynthetic() {
    return (access & Opcodes.ACC_SYNTHETIC) != 0;
  }

  public boolean isVarargs() {
    return (access & Opcodes.ACC_VARARGS) != 0;
  }

  @Nullable
  public MethodNode getMethodNode() {
    return methodNode;
  }
}
