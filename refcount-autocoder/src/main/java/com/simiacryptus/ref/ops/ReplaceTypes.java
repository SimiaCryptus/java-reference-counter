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

package com.simiacryptus.ref.ops;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.wrappers.*;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.stream.*;

@RefIgnore
public class ReplaceTypes extends RefASTOperator {

  protected Map<String, String> replacements;

  protected ReplaceTypes(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, boolean invert) {
    super(projectInfo, compilationUnit, file);
    this.replacements = classMapping(invert);
  }

  protected ArrayList<Object> getTypes() {
    final ArrayList<Object> names = new ArrayList<>();
    compilationUnit.accept(new ASTVisitor() {
      @Override
      public void endVisit(TypeDeclarationStatement node) {
        names.add(node.resolveBinding().getQualifiedName());
      }

    });
    return names;
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
    replacements.put(AbstractCollection.class, RefAbstractCollection.class);
    replacements.put(AbstractList.class, RefAbstractList.class);
    replacements.put(AbstractMap.class, RefAbstractMap.class);
    replacements.put(AbstractSet.class, RefAbstractSet.class);
    replacements.put(ArrayList.class, RefArrayList.class);
    replacements.put(Arrays.class, RefArrays.class);
    replacements.put(Assert.class, RefAssert.class);
    replacements.put(Collection.class, RefCollection.class);
    replacements.put(Collections.class, RefCollections.class);
    replacements.put(Collectors.class, RefCollectors.class);
    replacements.put(Comparator.class, RefComparator.class);
    replacements.put(ConcurrentHashMap.class, RefConcurrentHashMap.class);
    replacements.put(ConcurrentLinkedDeque.class, RefConcurrentLinkedDeque.class);
    replacements.put(Consumer.class, RefConsumer.class);
    replacements.put(Deque.class, RefDeque.class);
    replacements.put(DoubleStream.class, RefDoubleStream.class);
    replacements.put(Map.Entry.class, RefEntry.class);
    replacements.put(WeakReference.class, RefWeakReference.class);
    replacements.put(HashMap.class, RefHashMap.class);
    replacements.put(TreeMap.class, RefTreeMap.class);
    replacements.put(HashSet.class, RefHashSet.class);
    replacements.put(IntStream.class, RefIntStream.class);
    replacements.put(Iterator.class, RefIterator.class);
    replacements.put(LinkedHashMap.class, RefLinkedHashMap.class);
    replacements.put(LinkedList.class, RefLinkedList.class);
    replacements.put(List.class, RefList.class);
    replacements.put(ListIterator.class, RefListIterator.class);
    replacements.put(Lists.class, RefLists.class);
    replacements.put(LongStream.class, RefLongStream.class);
    replacements.put(Map.class, RefMap.class);
    replacements.put(PrimitiveIterator.class, RefPrimitiveIterator.class);
    replacements.put(PrimitiveIterator.OfDouble.class, RefPrimitiveIterator.OfDouble.class);
    replacements.put(PrimitiveIterator.OfInt.class, RefPrimitiveIterator.OfInt.class);
    replacements.put(PrimitiveIterator.OfLong.class, RefPrimitiveIterator.OfLong.class);
    replacements.put(Queue.class, RefQueue.class);
    replacements.put(Set.class, RefSet.class);
    replacements.put(Spliterator.class, RefSpliterator.class);
    replacements.put(Spliterators.class, RefSpliterators.class);
    replacements.put(Stream.class, RefStream.class);
    replacements.put(StreamSupport.class, RefStreamSupport.class);
    replacements.put(TreeSet.class, RefTreeSet.class);
    return replacements;
  }

  public void apply(Name node) {
    if (node.getParent() instanceof ImportDeclaration) {
      return;
    }
    final MethodDeclaration methodDeclaration = getMethodDeclaration(node);
    if (null != methodDeclaration) {
      final IMethodBinding methodBinding = methodDeclaration.resolveBinding();
      if (null == methodBinding) {
        warn(methodDeclaration, "Cannot resolve %s", methodDeclaration);
      } else {
        final ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (!ASTUtil.hasAnnotation(declaringClass, RefAware.class)) {
          info(methodDeclaration, "Method %s is defined by %s, which is NOT ref-aware", methodBinding, declaringClass.getQualifiedName());
          //return;
        } else {
          info(methodDeclaration, "Method %s is defined by %s, which is ref-aware", methodBinding, declaringClass.getQualifiedName());
        }
      }
    }
    final Name replace = replace(node);
    if (null != replace && !node.toString().equals(replace.toString())) {
      replace(node, replace);
      info(node, "Replaced %s with %s", node, replace);
    }
  }

  public MethodDeclaration getMethodDeclaration(ASTNode node) {
    if (node instanceof MethodDeclaration) return (MethodDeclaration) node;
    if (node instanceof Statement) return null;
    if (node instanceof TypeDeclaration) return null;
    final ASTNode parent = node.getParent();
    if (null != parent) return getMethodDeclaration(parent);
    return null;
  }

  protected Name replace(Name node) {
    final IBinding binding = resolveBinding(node);
    if (null == binding) {
      warn(node, "Unresolved binding: %s", node);
    }
    if (binding instanceof ITypeBinding) {
      final String className = ((ITypeBinding) binding).getBinaryName();
      final String replacement = replacements.get(className);
      if (null != replacement) {
        try {
          return ASTUtil.newQualifiedName(ast, Class.forName(replacement));
        } catch (ClassNotFoundException e) {
          warn(node, e.getMessage());
        }
      }
    }
    return null;
  }

  public static class ModifyCompilationUnit extends ReplaceTypes {
    public ModifyCompilationUnit(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    @Override
    public void endVisit(CompilationUnit node) {
      final Iterator<ImportDeclaration> iterator = node.imports().iterator();
      ArrayList<ImportDeclaration> newImports = new ArrayList<>();
      while (iterator.hasNext()) {
        final ImportDeclaration importDeclaration = iterator.next();
        final Name name = importDeclaration.getName();
        final Name replace = replace(name);
        if (null != replace && !name.toString().equals(replace.toString())) {
          final ImportDeclaration newImportDeclaration = ast.newImportDeclaration();
          newImportDeclaration.setName(replace);
          newImports.add(newImportDeclaration);
        }
      }
      node.imports().addAll(newImports);
    }
  }

  public static class ModifyTypeParameter extends ReplaceTypes {
    public ModifyTypeParameter(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    @Override
    public void endVisit(TypeParameter node) {
      if (skip(node)) return;
      apply(node.getName());
    }
  }

  public static class ModifySimpleName extends ReplaceTypes {
    public ModifySimpleName(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    @Override
    public void endVisit(SimpleName node) {
      if (skip(node)) return;
      if (node.getParent() instanceof QualifiedName) return;
      apply(node);
    }
  }

  public static class ModifyQualifiedName extends ReplaceTypes {
    public ModifyQualifiedName(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    @Override
    public void endVisit(QualifiedName node) {
      if (skip(node)) return;
      if (node.getParent() instanceof QualifiedName) return;
      apply(node);
    }
  }

}
