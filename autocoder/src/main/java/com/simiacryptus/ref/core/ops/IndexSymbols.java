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

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The type Index symbols.
 */
public class IndexSymbols extends ASTScanner {

  /**
   * The Index.
   */
  public final SymbolIndex index;
  private boolean verbose = true;
  private boolean failOnDuplicate;

  /**
   * Instantiates a new Index symbols.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   * @param index           the index
   */
  public IndexSymbols(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file, SymbolIndex index) {
    this(projectInfo, compilationUnit, file, index, true);
  }

  /**
   * Instantiates a new Index symbols.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   * @param index           the index
   * @param failOnDuplicate the fail on duplicate
   */
  public IndexSymbols(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file, SymbolIndex index, boolean failOnDuplicate) {
    super(projectInfo, compilationUnit, file, false);
    this.index = index;
    this.failOnDuplicate = failOnDuplicate;
  }

  /**
   * Is verbose boolean.
   *
   * @return the boolean
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets verbose.
   *
   * @param verbose the verbose
   * @return the verbose
   */
  @NotNull
  public IndexSymbols setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  /**
   * Context linked hash map.
   *
   * @param node the node
   * @return the linked hash map
   */
  @NotNull
  public LinkedHashMap<SymbolIndex.BindingID, Span> context(@NotNull ASTNode node) {
    final LinkedHashMap<SymbolIndex.BindingID, Span> list = new LinkedHashMap<>();
    final ASTNode parent = node.getParent();
    if (parent != null) list.putAll(context(parent));
    if (node instanceof LambdaExpression) {
      final IMethodBinding methodBinding = ((LambdaExpression) node).resolveMethodBinding();
      if (methodBinding == null) {
        warn(node, "Unresolved binding for %s", node);
      } else {
        list.put(index.getBindingID(methodBinding), getSpan(node));
      }
    } else if (node instanceof MethodDeclaration) {
      final IMethodBinding methodBinding = ((MethodDeclaration) node).resolveBinding();
      if (methodBinding == null) {
        warn(node, "Unresolved binding for %s", node);
      } else {
        list.put(index.getBindingID(methodBinding), getSpan(node));
      }
    } else if (node instanceof TypeDeclaration) {
      final ITypeBinding typeBinding = ((TypeDeclaration) node).resolveBinding();
      if (typeBinding == null) {
        warn(node, "Unresolved binding for %s", node);
      } else {
        list.put(index.getBindingID(typeBinding), getSpan(node));
      }
    }
    return list;
  }

  @Override
  public void endVisit(@NotNull LambdaExpression node) {
    indexDef(node, node.resolveMethodBinding());
  }

  @Override
  public void endVisit(@NotNull MethodDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  @Override
  public void endVisit(@NotNull TypeDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  @Override
  public void endVisit(@NotNull SingleVariableDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  @Override
  public void endVisit(@NotNull VariableDeclarationFragment node) {
    if (!(node.getParent() instanceof FieldDeclaration)) {
      indexDef(node, node.resolveBinding());
    }
  }

  @Override
  public void endVisit(@NotNull FieldDeclaration node) {
    final List fragments = node.fragments();
    for (Object fragment : fragments) {
      if (fragment instanceof VariableDeclarationFragment) {
        final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
        indexDef(node, variableDeclarationFragment.resolveBinding());
      } else {
        if (isVerbose()) debug(node, "Other fragment type %s", fragment.getClass().getSimpleName());
      }
    }
  }

  @Override
  public void endVisit(@NotNull QualifiedName node) {
    final ITypeBinding qualifierType = node.getQualifier().resolveTypeBinding();
    if (null != qualifierType && qualifierType.isArray()) return;
    final IBinding binding = node.resolveBinding();
    if (null != binding) {
      indexReference(node, binding);
    }
    super.endVisit(node);
  }

  @Override
  public void endVisit(@NotNull SimpleName node) {
    if (!(node.getParent() instanceof QualifiedName)) {
      final IBinding binding = node.resolveBinding();
      if (null != binding) {
        indexReference(node, binding);
      }
    }
    super.endVisit(node);
  }

  /**
   * Index reference.
   *
   * @param node    the node
   * @param binding the binding
   */
  public void indexReference(@NotNull Name node, @Nullable IBinding binding) {
    if (null == binding) {
      if (isVerbose()) debug(node, "Unresolved element for %s", binding.getName());
      return;
    }
    SymbolIndex.BindingID bindingID = index.getBindingID(binding);
    if (null == bindingID) return;
    final SymbolIndex.ContextLocation contextLocation = getContextLocation(node);
    final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
    if (isVerbose()) debug(node, "Reference to %s at %s within:\n\t%s", bindingID, contextLocation.location, contextPath);
    index.references.computeIfAbsent(bindingID, x -> new ArrayList<>()).add(node);
  }

  /**
   * Gets context location.
   *
   * @param node the node
   * @return the context location
   */
  @NotNull
  protected SymbolIndex.ContextLocation getContextLocation(@NotNull ASTNode node) {
    return new SymbolIndex.ContextLocation(getSpan(node), context(node));
  }

  private void indexDef(@NotNull ASTNode node, @Nullable IBinding binding) {
    if (null == binding) return;
    final SymbolIndex.BindingID bindingID = index.getBindingID(binding);
    if (null == bindingID) return;
    final SymbolIndex.ContextLocation contextLocation = getContextLocation(node);
    final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
    if (isVerbose()) debug(node, "Declaration of %s at %s within: \n\t%s", bindingID, getSpan(node), contextPath);
    final ASTNode replaced = index.definitions.put(bindingID, node);
    if (null != replaced) {
      if (failOnDuplicate) throw new RuntimeException(String.format("Duplicate declaration of %s in %s and %s", bindingID, getSpan(replaced), contextLocation.location));
      else warn(node, "Duplicate declaration of %s in %s and %s", bindingID, getSpan(replaced), contextLocation.location);
    }
  }

}
