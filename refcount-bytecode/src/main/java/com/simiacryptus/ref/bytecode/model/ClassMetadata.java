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
import com.simiacryptus.ref.lang.RefIgnore;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@RefIgnore
public final class ClassMetadata {
  @Nonnull
  private final String internalName;
  @Nullable
  private final String superName;
  @Nonnull
  private final List<String> interfaces;
  private final int access;
  @Nullable
  private final String signature;
  @Nullable
  private final String sourceFile;
  @Nullable
  private final String originPath;
  @Nonnull
  private final SourceLanguage language;
  private final boolean refAware;
  private final boolean refIgnore;
  private final boolean nonStaticInner;
  private final boolean anonymousOrClosure;
  @Nullable
  private final String outerInternalName;
  @Nonnull
  private final List<FieldMetadata> fields;
  @Nonnull
  private final Map<String, FieldMetadata> fieldsByName;
  @Nonnull
  private final List<MethodMetadata> methods;
  @Nonnull
  private final Map<String, MethodMetadata> methodsByKey;
  @Nullable
  private final ClassNode classNode;

  public ClassMetadata(
      @Nonnull String internalName,
      @Nullable String superName,
      @Nonnull List<String> interfaces,
      int access,
      @Nullable String signature,
      @Nullable String sourceFile,
      @Nullable String originPath,
      @Nonnull SourceLanguage language,
      boolean refAware,
      boolean refIgnore,
      boolean nonStaticInner,
      boolean anonymousOrClosure,
      @Nullable String outerInternalName,
      @Nonnull List<FieldMetadata> fields,
      @Nonnull List<MethodMetadata> methods,
      @Nullable ClassNode classNode
  ) {
    this.internalName = internalName;
    this.superName = superName;
    this.interfaces = Collections.unmodifiableList(new ArrayList<>(interfaces));
    this.access = access;
    this.signature = signature;
    this.sourceFile = sourceFile;
    this.originPath = originPath;
    this.language = language;
    this.refAware = refAware;
    this.refIgnore = refIgnore;
    this.nonStaticInner = nonStaticInner;
    this.anonymousOrClosure = anonymousOrClosure;
    this.outerInternalName = outerInternalName;
    this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    Map<String, FieldMetadata> fMap = new LinkedHashMap<>();
    for (FieldMetadata f : fields) {
      fMap.put(f.getName(), f);
    }
    this.fieldsByName = Collections.unmodifiableMap(fMap);
    this.methods = Collections.unmodifiableList(new ArrayList<>(methods));
    Map<String, MethodMetadata> mMap = new LinkedHashMap<>();
    for (MethodMetadata m : methods) {
      mMap.put(methodKey(m.getName(), m.getDescriptor()), m);
    }
    this.methodsByKey = Collections.unmodifiableMap(mMap);
    this.classNode = classNode;
  }

  @Nonnull
  public static String methodKey(@Nonnull String name, @Nonnull String descriptor) {
    return name + descriptor;
  }

  @Nonnull
  public static ClassMetadata fromAsm(@Nonnull ClassNode node, @Nullable String originPath, boolean retainNode) {
    SourceLanguage lang = AnnotationUtil.detectLanguage(node);
    boolean refAware = AnnotationUtil.hasAnnotation(
        node.visibleAnnotations, node.invisibleAnnotations, AnnotationUtil.DESC_REF_AWARE
    );
    boolean refIgnore = AnnotationUtil.hasAnnotation(
        node.visibleAnnotations, node.invisibleAnnotations, AnnotationUtil.DESC_REF_IGNORE
    );

    boolean isAnonymous = false;
    boolean isNonStaticInner = false;
    String outer = node.outerClass;

    if (node.innerClasses != null) {
      for (InnerClassNode ic : node.innerClasses) {
        if (node.name.equals(ic.name)) {
          if (ic.innerName == null) {
            isAnonymous = true;
          }
          if (outer == null && ic.outerName != null) {
            outer = ic.outerName;
          }
          boolean staticInner = (ic.access & Opcodes.ACC_STATIC) != 0
              || (ic.access & Opcodes.ACC_INTERFACE) != 0
              || (ic.access & Opcodes.ACC_ENUM) != 0;
          if (!staticInner && (ic.outerName != null || node.outerClass != null)) {
            isNonStaticInner = true;
          }
          break;
        }
      }
    }

    // Fallback check for synthetic outer reference field (e.g. this$0 in Java/Kotlin/Scala inner classes)
    boolean hasOuterField = false;
    if (node.fields != null) {
      for (FieldNode f : node.fields) {
        if ((f.access & Opcodes.ACC_SYNTHETIC) != 0 && f.name.startsWith("this$")) {
          hasOuterField = true;
          break;
        }
      }
    }
    if (hasOuterField && !isAnonymous) {
      isNonStaticInner = true;
    }

    // Detect Kotlin/Scala compiled closure classes (e.g. extending kotlin/jvm/internal/Lambda or scala/runtime/AbstractFunction*)
    boolean isClosureSuper = "kotlin/jvm/internal/Lambda".equals(node.superName)
        || (node.superName != null && node.superName.startsWith("scala/runtime/AbstractFunction"))
        || node.name.contains("$$Lambda")
        || node.name.contains("$anonfun$");
    boolean anonymousOrClosure = isAnonymous || isClosureSuper;

    List<FieldMetadata> fieldList = new ArrayList<>();
    if (node.fields != null) {
      for (FieldNode f : node.fields) {
        fieldList.add(FieldMetadata.fromAsm(f));
      }
    }

    List<MethodMetadata> methodList = new ArrayList<>();
    if (node.methods != null) {
      for (MethodNode m : node.methods) {
        methodList.add(MethodMetadata.fromAsm(node.name, m, isNonStaticInner && !isAnonymous, retainNode));
      }
    }

    List<String> ifaces = node.interfaces != null ? node.interfaces : Collections.emptyList();
    return new ClassMetadata(
        node.name,
        node.superName,
        ifaces,
        node.access,
        node.signature,
        node.sourceFile,
        originPath,
        lang,
        refAware,
        refIgnore,
        isNonStaticInner && !anonymousOrClosure,
        anonymousOrClosure,
        outer,
        fieldList,
        methodList,
        retainNode ? node : null
    );
  }

  @Nonnull
  public String getInternalName() {
    return internalName;
  }

  @Nonnull
  public String getBinaryName() {
    return internalName.replace('/', '.');
  }

  @Nullable
  public String getSuperName() {
    return superName;
  }

  @Nonnull
  public List<String> getInterfaces() {
    return interfaces;
  }

  public int getAccess() {
    return access;
  }

  @Nullable
  public String getSignature() {
    return signature;
  }

  @Nullable
  public String getSourceFile() {
    return sourceFile;
  }

  @Nullable
  public String getOriginPath() {
    return originPath;
  }

  @Nonnull
  public SourceLanguage getLanguage() {
    return language;
  }

  public boolean isRefAware() {
    return refAware;
  }

  public boolean isRefIgnore() {
    return refIgnore;
  }

  public boolean isNonStaticInner() {
    return nonStaticInner;
  }

  public boolean isAnonymousOrClosure() {
    return anonymousOrClosure;
  }

  @Nullable
  public String getOuterInternalName() {
    return outerInternalName;
  }

  public boolean isInterface() {
    return (access & Opcodes.ACC_INTERFACE) != 0;
  }

  public boolean isAbstract() {
    return (access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public boolean isFinal() {
    return (access & Opcodes.ACC_FINAL) != 0;
  }

  public boolean isSynthetic() {
    return (access & Opcodes.ACC_SYNTHETIC) != 0;
  }

  public boolean isEnum() {
    return (access & Opcodes.ACC_ENUM) != 0;
  }

  public boolean isAnnotation() {
    return (access & Opcodes.ACC_ANNOTATION) != 0;
  }

  public boolean isModuleInfo() {
    return (access & Opcodes.ACC_MODULE) != 0 || "module-info".equals(internalName);
  }

  @Nonnull
  public List<FieldMetadata> getFields() {
    return fields;
  }

  @Nullable
  public FieldMetadata findField(@Nonnull String name) {
    return fieldsByName.get(name);
  }

  @Nonnull
  public List<MethodMetadata> getMethods() {
    return methods;
  }

  @Nullable
  public MethodMetadata findMethod(@Nonnull String name, @Nonnull String descriptor) {
    return methodsByKey.get(methodKey(name, descriptor));
  }

  @Nullable
  public ClassNode getClassNode() {
    return classNode;
  }
}
