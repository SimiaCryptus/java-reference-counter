/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.ops;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.wrappers.*;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.*;

@RefIgnore
public class ReplaceTypes extends RefFileAstVisitor {

  private Map<String, String> replacements;

  public ReplaceTypes(CompilationUnit compilationUnit, File file, boolean invert) {
    this(compilationUnit, file, classMapping(invert));
  }

  ReplaceTypes(CompilationUnit compilationUnit, File file, Map<String, String> classMapping) {
    super(compilationUnit, file);
    this.replacements = classMapping;
  }

  public static Map<String, String> classMapping(boolean invert) {
    return (invert ? classMapping().inverse() : classMapping())
        .entrySet().stream().collect(Collectors.toMap(
            x -> x.getKey().getCanonicalName(),
            x -> x.getValue().getCanonicalName()));
  }

  @NotNull
  public static BiMap<Class<?>, Class<?>> classMapping() {
    BiMap<Class<?>, Class<?>> replacements = HashBiMap.create();
    replacements.put(Stream.class, RefStream.class);
    replacements.put(IntStream.class, RefIntStream.class);
    replacements.put(LongStream.class, RefLongStream.class);
    replacements.put(DoubleStream.class, RefDoubleStream.class);
    replacements.put(ConcurrentLinkedDeque.class, RefConcurrentLinkedDeque.class);
    replacements.put(Arrays.class, RefArrays.class);
    replacements.put(ArrayList.class, RefArrayList.class);
    replacements.put(List.class, RefList.class);
    replacements.put(HashMap.class, RefHashMap.class);
    replacements.put(ConcurrentHashMap.class, RefConcurrentHashMap.class);
    replacements.put(LinkedHashMap.class, RefLinkedHashMap.class);
    replacements.put(LinkedList.class, RefLinkedList.class);
    replacements.put(HashSet.class, RefHashSet.class);
    replacements.put(TreeSet.class, RefTreeSet.class);
    replacements.put(Collectors.class, RefCollectors.class);
    replacements.put(Comparator.class, RefComparator.class);
    replacements.put(StreamSupport.class, RefStreamSupport.class);
    replacements.put(Lists.class, RefLists.class);
    return replacements;
  }


  public void apply(Name node) {
    if (skip(node)) return;
    final IBinding binding = node.resolveBinding();
    if (null == binding) {
      warn(node, "Unresolved binding: %s", node);
    }
    if (binding instanceof ITypeBinding) {
      final String className = ((ITypeBinding) binding).getBinaryName();
      final String replacement = replacements.get(className);
      if (null != replacement) {
        try {
          replace(node, newQualifiedName(node.getAST(), Class.forName(replacement)));
          info(node, "Replaced %s with %s", className, replacement);
        } catch (ClassNotFoundException e) {
          warn(node, e.getMessage());
        }
      }
    }
  }

  @Override
  public void endVisit(TypeParameter node) {
    apply(node.getName());
  }

  @Override
  public void endVisit(SimpleName node) {
    if (node.getParent() instanceof QualifiedName) return;
    apply(node);
  }

  @Override
  public void endVisit(QualifiedName node) {
    apply(node);
  }

}
