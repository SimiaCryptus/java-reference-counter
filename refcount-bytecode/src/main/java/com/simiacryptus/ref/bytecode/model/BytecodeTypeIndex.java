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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

@RefIgnore
public final class BytecodeTypeIndex implements AutoCloseable {
  public static final String INTERNAL_REF_COUNTING = Type.getInternalName(ReferenceCounting.class);
  public static final String INTERNAL_REF_COUNTING_BASE = Type.getInternalName(ReferenceCountingBase.class);
  public static final String INTERNAL_REF_UTIL = Type.getInternalName(RefUtil.class);
  public static final String INTERNAL_OPTIONAL = "java/util/Optional";
  public static final String INTERNAL_MAP_ENTRY = "java/util/Map$Entry";
  public static final String INTERNAL_OBJECT = "java/lang/Object";

  private static final ClassMetadata MISSING = new ClassMetadata(
      "__missing__", null, Collections.emptyList(), 0, null, null, null,
      com.simiacryptus.ref.bytecode.SourceLanguage.BYTECODE,
      false, false, false, false, null,
      Collections.emptyList(), Collections.emptyList(), null
  );

  @Nonnull
  private final Map<String, ClassMetadata> targetClasses = new LinkedHashMap<>();
  @Nonnull
  private final Map<String, ClassMetadata> classpathCache = new ConcurrentHashMap<>();
  @Nonnull
  private final List<File> classpathDirectories = new ArrayList<>();
  @Nonnull
  private final List<JarFile> openJars = new ArrayList<>();
  @Nonnull
  private final Map<String, JarFile> jarClassIndex = new HashMap<>();
  @Nullable
  private final ClassLoader fallbackLoader;

  public BytecodeTypeIndex(@Nullable ClassLoader fallbackLoader) {
    this.fallbackLoader = fallbackLoader;
  }

  public void addTargetBytes(@Nonnull byte[] classBytes, @Nullable String originPath) {
    ClassNode node = new ClassNode();
    ClassReader reader = new ClassReader(classBytes);
    reader.accept(node, ClassReader.EXPAND_FRAMES);
    ClassMetadata metadata = ClassMetadata.fromAsm(node, originPath, true);
    targetClasses.put(metadata.getInternalName(), metadata);
    classpathCache.put(metadata.getInternalName(), metadata);
  }

  public void addTargetFile(@Nonnull File file) throws IOException {
    if (!file.exists()) return;
    if (file.isDirectory()) {
      try (Stream<Path> walk = Files.walk(file.toPath())) {
        List<Path> classFiles = new ArrayList<>();
        walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".class"))
            .sorted()
            .forEach(classFiles::add);
        for (Path classFile : classFiles) {
          addTargetBytes(Files.readAllBytes(classFile), classFile.toAbsolutePath().toString());
        }
      }
    } else {
      String lower = file.getName().toLowerCase();
      if (lower.endsWith(".class")) {
        addTargetBytes(Files.readAllBytes(file.toPath()), file.getAbsolutePath());
      } else if (lower.endsWith(".jar") || lower.endsWith(".zip") || lower.endsWith(".jmod")) {
        try (JarFile jar = new JarFile(file)) {
          Enumeration<JarEntry> entries = jar.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (!entry.isDirectory() && entryName.endsWith(".class") && !entryName.startsWith("META-INF/versions/")) {
              try (InputStream in = jar.getInputStream(entry)) {
                byte[] bytes = in.readAllBytes();
                addTargetBytes(bytes, file.getAbsolutePath() + "!" + entryName);
              }
            }
          }
        }
      }
    }
  }

  public void addClasspathEntry(@Nonnull File file) throws IOException {
    if (!file.exists()) return;
    if (file.isDirectory()) {
      classpathDirectories.add(file);
    } else {
      String lower = file.getName().toLowerCase();
      if (lower.endsWith(".jar") || lower.endsWith(".zip")) {
        JarFile jar = new JarFile(file);
        openJars.add(jar);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String name = entry.getName();
          if (!entry.isDirectory() && name.endsWith(".class") && !name.startsWith("META-INF/versions/")) {
            String internalName = name.substring(0, name.length() - ".class".length());
            jarClassIndex.putIfAbsent(internalName, jar);
          }
        }
      }
    }
  }

  @Nonnull
  public Collection<ClassMetadata> getTargetClasses() {
    return Collections.unmodifiableCollection(targetClasses.values());
  }

  @Nullable
  public ClassMetadata resolveClass(@Nullable String internalName) {
    if (internalName == null || internalName.isEmpty() || internalName.startsWith("[")) {
      return null;
    }
    ClassMetadata existing = classpathCache.get(internalName);
    if (existing != null) {
      return existing == MISSING ? null : existing;
    }
    ClassMetadata loaded = loadClasspathMetadata(internalName);
    classpathCache.put(internalName, loaded != null ? loaded : MISSING);
    return loaded;
  }

  @Nullable
  private ClassMetadata loadClasspathMetadata(@Nonnull String internalName) {
    String relPath = internalName + ".class";
    for (File dir : classpathDirectories) {
      File candidate = new File(dir, relPath.replace('/', File.separatorChar));
      if (candidate.isFile()) {
        try {
          byte[] bytes = Files.readAllBytes(candidate.toPath());
          return parseHeaderOnly(bytes, candidate.getAbsolutePath());
        } catch (IOException ignored) {
        }
      }
    }
    JarFile jar = jarClassIndex.get(internalName);
    if (jar != null) {
      JarEntry entry = jar.getJarEntry(relPath);
      if (entry != null) {
        try (InputStream in = jar.getInputStream(entry)) {
          byte[] bytes = in.readAllBytes();
          return parseHeaderOnly(bytes, jar.getName() + "!" + relPath);
        } catch (IOException ignored) {
        }
      }
    }
    ClassLoader loader = fallbackLoader != null ? fallbackLoader : ClassLoader.getSystemClassLoader();
    if (loader != null) {
      try (InputStream in = loader.getResourceAsStream(relPath)) {
        if (in != null) {
          byte[] bytes = in.readAllBytes();
          return parseHeaderOnly(bytes, "classpath:" + relPath);
        }
      } catch (IOException ignored) {
      }
    }
    return null;
  }

  @Nonnull
  private static ClassMetadata parseHeaderOnly(@Nonnull byte[] bytes, @Nullable String origin) {
    ClassNode node = new ClassNode();
    ClassReader reader = new ClassReader(bytes);
    reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return ClassMetadata.fromAsm(node, origin, false);
  }

  public boolean derivesFrom(@Nullable String internalName, @Nonnull String targetSuperInternalName) {
    if (internalName == null || internalName.isEmpty()) return false;
    if (internalName.equals(targetSuperInternalName)) return true;
    Set<String> visited = new HashSet<>();
    Deque<String> queue = new ArrayDeque<>();
    queue.add(internalName);
    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (!visited.add(current)) continue;
      if (current.equals(targetSuperInternalName)) {
        return true;
      }
      ClassMetadata meta = resolveClass(current);
      if (meta == null) continue;
      if (meta.getSuperName() != null) {
        queue.add(meta.getSuperName());
      }
      queue.addAll(meta.getInterfaces());
    }
    return false;
  }

  public boolean isReferenceCountingClass(@Nullable String internalName) {
    return derivesFrom(internalName, INTERNAL_REF_COUNTING);
  }

  public boolean isReferenceCountingBaseSubclass(@Nullable String internalName) {
    return derivesFrom(internalName, INTERNAL_REF_COUNTING_BASE);
  }

  public boolean isRefCountedType(@Nullable Type type, @Nullable String genericSignature) {
    if (type == null) return false;
    int sort = type.getSort();
    if (sort != Type.OBJECT && sort != Type.ARRAY) {
      return false;
    }
    if (sort == Type.ARRAY) {
      return isRefCountedType(type.getElementType(), null);
    }
    String internalName = type.getInternalName();
    if (derivesFrom(internalName, INTERNAL_REF_COUNTING)) {
      return true;
    }
    ClassMetadata meta = resolveClass(internalName);
    if (meta != null && meta.isRefAware()) {
      return true;
    }
    if (INTERNAL_OPTIONAL.equals(internalName)) {
      List<Type> typeArgs = SignatureUtil.extractTopLevelTypeArguments(genericSignature);
      if (!typeArgs.isEmpty()) {
        return isRefCountedType(typeArgs.get(0), null);
      }
      return false;
    }
    if (derivesFrom(internalName, INTERNAL_MAP_ENTRY)) {
      List<Type> typeArgs = SignatureUtil.extractTopLevelTypeArguments(genericSignature);
      if (!typeArgs.isEmpty()) {
        for (Type arg : typeArgs) {
          if (isRefCountedType(arg, null)) {
            return true;
          }
        }
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Finds all super declarations (superclasses and interfaces) of a method with matching name and descriptor.
   */
  @Nonnull
  public List<MethodMetadata> findSuperMethods(
      @Nonnull String ownerInternalName,
      @Nonnull String methodName,
      @Nonnull String methodDesc
  ) {
    if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
      return Collections.emptyList();
    }
    List<MethodMetadata> supers = new ArrayList<>();
    ClassMetadata start = resolveClass(ownerInternalName);
    if (start == null) return supers;

    Set<String> visited = new HashSet<>();
    Deque<String> queue = new ArrayDeque<>();
    if (start.getSuperName() != null) {
      queue.add(start.getSuperName());
    }
    queue.addAll(start.getInterfaces());

    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (!visited.add(current)) continue;
      ClassMetadata meta = resolveClass(current);
      if (meta == null) continue;
      MethodMetadata found = meta.findMethod(methodName, methodDesc);
      if (found != null && !found.isStatic()) {
        supers.add(found);
      }
      if (meta.getSuperName() != null) {
        queue.add(meta.getSuperName());
      }
      queue.addAll(meta.getInterfaces());
    }
    return supers;
  }

  /**
   * Resolves the effective annotation contract of a method across its receiver type and all super-declarations.
   */
  @Nonnull
  public MethodContract resolveMethodContract(
      @Nonnull String ownerInternalName,
      @Nonnull String methodName,
      @Nonnull String methodDesc
  ) {
    Type[] argTypes = Type.getArgumentTypes(methodDesc);
    int paramCount = argTypes.length;
    boolean[] pAware = new boolean[paramCount];
    boolean[] pIgnore = new boolean[paramCount];
    boolean[] pSynth = new boolean[paramCount];
    boolean resolved = false;
    boolean methodIgnore = false;
    boolean returnAware = false;
    boolean mustCall = false;
    boolean varargs = false;

    if (ownerInternalName.startsWith("[")) {
      // Array methods such as clone()
      return new MethodContract(true, false, false, false, false, pAware, pIgnore, pSynth);
    }

    ClassMetadata ownerMeta = resolveClass(ownerInternalName);
    if (ownerMeta != null) {
      MethodMetadata direct = ownerMeta.findMethod(methodName, methodDesc);
      if (direct != null) {
        resolved = true;
        methodIgnore |= direct.isRefIgnore();
        returnAware |= direct.isRefAware();
        mustCall |= direct.isMustCall();
        varargs |= direct.isVarargs();
        for (int i = 0; i < paramCount; i++) {
          pAware[i] |= direct.isParameterRefAware(i);
          pIgnore[i] |= direct.isParameterRefIgnore(i);
          pSynth[i] |= direct.isParameterSynthetic(i);
        }
      }
    }

    if (!"<init>".equals(methodName) && !"<clinit>".equals(methodName)) {
      List<MethodMetadata> supers = findSuperMethods(ownerInternalName, methodName, methodDesc);
      for (MethodMetadata sup : supers) {
        resolved = true;
        returnAware |= sup.isRefAware();
        mustCall |= sup.isMustCall();
        varargs |= sup.isVarargs();
        for (int i = 0; i < paramCount; i++) {
          pAware[i] |= sup.isParameterRefAware(i);
          pIgnore[i] |= sup.isParameterRefIgnore(i);
        }
      }
    }

    return new MethodContract(resolved, methodIgnore, returnAware, mustCall, varargs, pAware, pIgnore, pSynth);
  }

  @Override
  public void close() {
    for (JarFile jar : openJars) {
      try {
        jar.close();
      } catch (IOException ignored) {
      }
    }
    openJars.clear();
  }
}
