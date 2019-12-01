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

package com.simiacryptus.devutil.ref;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.simiacryptus.lang.ref.wrappers.*;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ReplaceTypes extends RefFileAstVisitor {

  private Map<String, String> replacements;

  ReplaceTypes(CompilationUnit compilationUnit, File file, boolean invert) {
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
    replacements.put(Arrays.class, RefArrays.class);
    replacements.put(ArrayList.class, RefArrayList.class);
    replacements.put(List.class, RefList.class);
    replacements.put(HashMap.class, RefHashMap.class);
    replacements.put(HashSet.class, RefHashSet.class);
    replacements.put(Collectors.class, RefCollectors.class);
    replacements.put(Comparator.class, RefComparator.class);
    return replacements;
  }

  public void apply(Name node) {
    if (skip(node)) return;
    final IBinding binding = node.resolveBinding();
    if (binding instanceof ITypeBinding) {
      final String className = ((ITypeBinding) binding).getBinaryName();
      final String replacement = replacements.get(className);
      if (null != replacement) {
        try {
          replace(node, newQualifiedName(node.getAST(), Class.forName(replacement)));
        } catch (ClassNotFoundException e) {
          warn(node, e.getMessage());
        }
      }
    }
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
