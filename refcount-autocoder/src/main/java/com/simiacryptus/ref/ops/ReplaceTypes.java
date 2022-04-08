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
import com.google.common.collect.Maps;
import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.wrappers.*;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.*;

/**
 * This class is responsible for replacing types in a map.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class ReplaceTypes extends RefASTOperator {

  protected Map<String, String> replacements;

  protected ReplaceTypes(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean invert) {
    super(projectInfo, compilationUnit, file);
    this.replacements = classMapping(invert);
  }

  /**
   * Returns an ArrayList of Objects.
   *
   * @return an ArrayList of Objects
   * @docgenVersion 9
   */
  @Nonnull
  @SuppressWarnings("unused")
  protected ArrayList<Object> getTypes() {
    final ArrayList<Object> names = new ArrayList<>();
    compilationUnit.accept(new ASTVisitor() {
      /**
       * This method is called when the end of a type declaration statement is reached in the Java code.
       * The fully qualified name of the type is added to the "names" list.
       *
       * @param node the type declaration statement node that is being visited
       *
       *   @docgenVersion 9
       */
      @Override
      public void endVisit(@Nonnull TypeDeclarationStatement node) {
        names.add(node.resolveBinding().getQualifiedName());
      }
    });
    return names;
  }

  /**
   * Returns a mapping of class names to other class names, optionally inverting the mapping.
   *
   * @param invert whether to invert the mapping
   * @return the mapping of class names
   * @docgenVersion 9
   */
  public static Map<String, String> classMapping(boolean invert) {
    return (invert ? classMapping().inverse() : classMapping())
        .entrySet().stream().collect(Collectors.toMap(
            x -> x.getKey().getCanonicalName(),
            x -> x.getValue().getCanonicalName()));
  }

  /**
   * Returns a BiMap mapping Classes to their corresponding Classes.
   *
   * @return a BiMap mapping Classes to their corresponding Classes
   * @docgenVersion 9
   */
  @Nonnull
  public static BiMap<Class<?>, Class<?>> classMapping() {
    BiMap<Class<?>, Class<?>> replacements = HashBiMap.create();
    replacements.put(AbstractCollection.class, RefAbstractCollection.class);
    replacements.put(AbstractList.class, RefAbstractList.class);
    replacements.put(AbstractMap.class, RefAbstractMap.class);
    replacements.put(AbstractSet.class, RefAbstractSet.class);
    replacements.put(ArrayList.class, RefArrayList.class);
    replacements.put(Arrays.class, RefArrays.class);
    replacements.put(Collection.class, RefCollection.class);
    replacements.put(LinkedBlockingQueue.class, RefLinkedBlockingQueue.class);
    replacements.put(Collections.class, RefCollections.class);
    replacements.put(Collectors.class, RefCollectors.class);
    replacements.put(Comparator.class, RefComparator.class);
    replacements.put(ConcurrentHashMap.class, RefConcurrentHashMap.class);
    replacements.put(ConcurrentLinkedDeque.class, RefConcurrentLinkedDeque.class);
    replacements.put(Consumer.class, RefConsumer.class);
    replacements.put(Deque.class, RefDeque.class);
    replacements.put(DoubleStream.class, RefDoubleStream.class);
    replacements.put(Map.Entry.class, RefEntry.class);
    replacements.put(Maps.class, RefMaps.class);
    replacements.put(WeakReference.class, RefWeakReference.class);
    replacements.put(HashMap.class, RefHashMap.class);
    replacements.put(NavigableMap.class, RefNavigableMap.class);
    replacements.put(NavigableSet.class, RefNavigableSet.class);
    replacements.put(SortedSet.class, RefSortedSet.class);
    replacements.put(SortedMap.class, RefSortedMap.class);
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
    replacements.put(Spliterator.OfDouble.class, RefSpliterator.OfDouble.class);
    replacements.put(Spliterator.OfInt.class, RefSpliterator.OfInt.class);
    replacements.put(Spliterator.OfLong.class, RefSpliterator.OfLong.class);
    replacements.put(Spliterators.class, RefSpliterators.class);
    replacements.put(Stream.class, RefStream.class);
    replacements.put(StreamSupport.class, RefStreamSupport.class);
    replacements.put(TreeSet.class, RefTreeSet.class);
    return replacements;
  }

  /**
   * Applies the given node.
   *
   * @param node the node to apply
   * @throws NullPointerException if the node is null
   * @docgenVersion 9
   */
  public void apply(@Nonnull Name node) {
    if (isImport(node)) {
      return;
    }
    final Name replace = replace(node);
    if (null != replace && !node.toString().equals(replace.toString())) {
      replace(node, replace);
      debug(node, "Replaced %s with %s", node, replace);
    }
  }

  /**
   * @param node the node to replace
   * @return the replaced node, or null if the node could not be replaced
   * @docgenVersion 9
   */
  @Nullable
  protected Name replace(@Nonnull Name node) {
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

  /**
   * Returns true if the given node is an import statement.
   *
   * @param node the node to check
   * @return true if the node is an import statement
   * @docgenVersion 9
   */
  private boolean isImport(@Nonnull ASTNode node) {
    if (node instanceof ImportDeclaration) return true;
    if (node instanceof Statement) return false;
    if (node instanceof TypeDeclaration) return false;
    final ASTNode parent = node.getParent();
    if (null == parent) return false;
    return isImport(parent);
  }

  /**
   * This class is responsible for modifying a compilation unit.
   *
   * @docgenVersion 9
   */
  public static class ModifyCompilationUnit extends ReplaceTypes {
    public ModifyCompilationUnit(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    /**
     * This method is called when the end of a compilation unit is reached in the tree walker.
     *
     * @param node the compilation unit node that is being visited
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull CompilationUnit node) {
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
      super.endVisit(node);
    }
  }

  /**
   * This class is used to modify a type parameter.
   *
   * @docgenVersion 9
   */
  public static class ModifyTypeParameter extends ReplaceTypes {
    public ModifyTypeParameter(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    /**
     * Visits a type parameter.
     *
     * @param node the type parameter to visit
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull TypeParameter node) {
      if (skip(node)) return;
      apply(node.getName());
    }
  }

  /**
   * This class is responsible for modifying a simple name.
   *
   * @docgenVersion 9
   */
  public static class ModifySimpleName extends ReplaceTypes {
    public ModifySimpleName(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    /**
     * This is the endVisit method for the SimpleName node.
     * If the node is skipped, the method will return.
     * If the node's parent is a QualifiedName, the method will return.
     * Otherwise, the node will be applied.
     *
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull SimpleName node) {
      if (skip(node)) return;
      if (node.getParent() instanceof QualifiedName) return;
      apply(node);
    }
  }

  /**
   * This class is responsible for modifying a qualified name.
   *
   * @docgenVersion 9
   */
  public static class ModifyQualifiedName extends ReplaceTypes {
    public ModifyQualifiedName(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, boolean invert) {
      super(projectInfo, compilationUnit, file, invert);
    }

    /**
     * This is the endVisit method for the QualifiedName node.
     * If the node is skipped, the method will return.
     * Otherwise, it will apply the node.
     *
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull QualifiedName node) {
      if (skip(node)) return;
      apply(node);
    }
  }

}
