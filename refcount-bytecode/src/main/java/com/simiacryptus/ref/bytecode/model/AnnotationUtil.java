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

import com.simiacryptus.ref.bytecode.SourceLanguage;
import com.simiacryptus.ref.lang.MustCall;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@RefIgnore
public final class AnnotationUtil {
  public static final String DESC_REF_AWARE = Type.getDescriptor(RefAware.class);
  public static final String DESC_REF_IGNORE = Type.getDescriptor(RefIgnore.class);
  public static final String DESC_MUST_CALL = Type.getDescriptor(MustCall.class);
  public static final String DESC_KOTLIN_METADATA = "Lkotlin/Metadata;";
  public static final String DESC_SCALA_SIGNATURE = "Lscala/reflect/ScalaSignature;";
  public static final String DESC_SCALA_LONG_SIGNATURE = "Lscala/reflect/ScalaLongSignature;";
  public static final String INTERNAL_GROOVY_OBJECT = "groovy/lang/GroovyObject";

  private AnnotationUtil() {
  }

  public static boolean hasAnnotation(
      @Nullable List<AnnotationNode> visible,
      @Nullable List<AnnotationNode> invisible,
      @Nonnull String descriptor
  ) {
    return containsAnnotation(visible, descriptor) || containsAnnotation(invisible, descriptor);
  }

  public static boolean hasTypeAnnotation(
      @Nullable List<TypeAnnotationNode> visible,
      @Nullable List<TypeAnnotationNode> invisible,
      @Nonnull String descriptor
  ) {
    return containsTypeAnnotation(visible, descriptor) || containsTypeAnnotation(invisible, descriptor);
  }

  private static boolean containsAnnotation(@Nullable List<AnnotationNode> list, @Nonnull String descriptor) {
    if (list == null) return false;
    for (AnnotationNode ann : list) {
      if (descriptor.equals(ann.desc)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsTypeAnnotation(@Nullable List<TypeAnnotationNode> list, @Nonnull String descriptor) {
    if (list == null) return false;
    for (TypeAnnotationNode ann : list) {
      if (descriptor.equals(ann.desc)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  public static SourceLanguage detectLanguage(@Nonnull ClassNode classNode) {
    if (hasAnnotation(classNode.visibleAnnotations, classNode.invisibleAnnotations, DESC_KOTLIN_METADATA)) {
      return SourceLanguage.KOTLIN;
    }
    if (hasAnnotation(classNode.visibleAnnotations, classNode.invisibleAnnotations, DESC_SCALA_SIGNATURE)
        || hasAnnotation(classNode.visibleAnnotations, classNode.invisibleAnnotations, DESC_SCALA_LONG_SIGNATURE)) {
      return SourceLanguage.SCALA;
    }
    if (classNode.interfaces != null && classNode.interfaces.contains(INTERNAL_GROOVY_OBJECT)) {
      return SourceLanguage.GROOVY;
    }
    if (classNode.sourceFile != null) {
      String lower = classNode.sourceFile.toLowerCase();
      if (lower.endsWith(".java")) return SourceLanguage.JAVA;
      if (lower.endsWith(".kt") || lower.endsWith(".kts")) return SourceLanguage.KOTLIN;
      if (lower.endsWith(".scala") || lower.endsWith(".sc")) return SourceLanguage.SCALA;
      if (lower.endsWith(".groovy") || lower.endsWith(".gvy")) return SourceLanguage.GROOVY;
    }
    return SourceLanguage.BYTECODE;
  }

  public static boolean hasParameterAnnotation(
      @Nonnull MethodNode method,
      int descriptorParamIndex,
      int totalDescriptorParams,
      @Nonnull String annotationDesc
  ) {
    if (checkParamAnnotationArray(
        method.visibleParameterAnnotations,
        method.visibleAnnotableParameterCount,
        descriptorParamIndex,
        totalDescriptorParams,
        annotationDesc
    )) {
      return true;
    }
    return checkParamAnnotationArray(
        method.invisibleParameterAnnotations,
        method.invisibleAnnotableParameterCount,
        descriptorParamIndex,
        totalDescriptorParams,
        annotationDesc
    );
  }

  private static boolean checkParamAnnotationArray(
      @Nullable List<AnnotationNode>[] paramAnns,
      int annotableCount,
      int descriptorParamIndex,
      int totalDescriptorParams,
      @Nonnull String annotationDesc
  ) {
    if (paramAnns == null || paramAnns.length == 0) {
      return false;
    }
    int effectiveAnnotable = annotableCount > 0 ? annotableCount : paramAnns.length;
    int syntheticPrefix = Math.max(0, totalDescriptorParams - effectiveAnnotable);
    int shiftedIndex = descriptorParamIndex - syntheticPrefix;
    if (shiftedIndex < 0 || shiftedIndex >= paramAnns.length) {
      return false;
    }
    return containsAnnotation(paramAnns[shiftedIndex], annotationDesc);
  }

  public static boolean isSyntheticParameter(
      @Nonnull MethodNode method,
      int descriptorParamIndex,
      int totalDescriptorParams,
      boolean nonStaticInnerConstructor
  ) {
    List<ParameterNode> parameters = method.parameters;
    if (parameters != null && descriptorParamIndex < parameters.size()) {
      ParameterNode paramNode = parameters.get(descriptorParamIndex);
      if (paramNode != null) {
        if ((paramNode.access & (org.objectweb.asm.Opcodes.ACC_SYNTHETIC | org.objectweb.asm.Opcodes.ACC_MANDATED)) != 0) {
          return true;
        }
        if (paramNode.name != null && (paramNode.name.startsWith("this$") || paramNode.name.startsWith("val$"))) {
          return true;
        }
      }
    }
    if ("<init>".equals(method.name)) {
      if (nonStaticInnerConstructor && descriptorParamIndex == 0) {
        return true;
      }
      int annotable = method.visibleAnnotableParameterCount > 0
          ? method.visibleAnnotableParameterCount
          : method.invisibleAnnotableParameterCount;
      if (annotable > 0 && annotable < totalDescriptorParams) {
        int syntheticPrefix = totalDescriptorParams - annotable;
        if (descriptorParamIndex < syntheticPrefix) {
          return true;
        }
      }
    }
    return false;
  }
}
