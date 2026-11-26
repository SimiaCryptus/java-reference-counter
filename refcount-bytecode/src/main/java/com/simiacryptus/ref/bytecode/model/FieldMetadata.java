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
import org.objectweb.asm.tree.FieldNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RefIgnore
public final class FieldMetadata {
  @Nonnull
  private final String name;
  @Nonnull
  private final String descriptor;
  @Nullable
  private final String signature;
  private final int access;
  private final boolean refAware;
  private final boolean refIgnore;

  public FieldMetadata(
      @Nonnull String name,
      @Nonnull String descriptor,
      @Nullable String signature,
      int access,
      boolean refAware,
      boolean refIgnore
  ) {
    this.name = name;
    this.descriptor = descriptor;
    this.signature = signature;
    this.access = access;
    this.refAware = refAware;
    this.refIgnore = refIgnore;
  }

  @Nonnull
  public static FieldMetadata fromAsm(@Nonnull FieldNode field) {
    boolean refAware = AnnotationUtil.hasAnnotation(
        field.visibleAnnotations, field.invisibleAnnotations, AnnotationUtil.DESC_REF_AWARE
    ) || AnnotationUtil.hasTypeAnnotation(
        field.visibleTypeAnnotations, field.invisibleTypeAnnotations, AnnotationUtil.DESC_REF_AWARE
    );
    boolean refIgnore = AnnotationUtil.hasAnnotation(
        field.visibleAnnotations, field.invisibleAnnotations, AnnotationUtil.DESC_REF_IGNORE
    ) || AnnotationUtil.hasTypeAnnotation(
        field.visibleTypeAnnotations, field.invisibleTypeAnnotations, AnnotationUtil.DESC_REF_IGNORE
    );
    return new FieldMetadata(field.name, field.desc, field.signature, field.access, refAware, refIgnore);
  }

  @Nonnull
  public String getName() {
    return name;
  }

  @Nonnull
  public String getDescriptor() {
    return descriptor;
  }

  @Nonnull
  public Type getType() {
    return Type.getType(descriptor);
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

  public boolean isStatic() {
    return (access & Opcodes.ACC_STATIC) != 0;
  }

  public boolean isFinal() {
    return (access & Opcodes.ACC_FINAL) != 0;
  }

  public boolean isSynthetic() {
    return (access & Opcodes.ACC_SYNTHETIC) != 0;
  }
}
