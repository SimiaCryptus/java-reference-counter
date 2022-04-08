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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for indexing symbols.
 *
 * @docgenVersion 9
 */
public class IndexSymbols extends ASTScanner {

  public final SymbolIndex index;
  private boolean verbose = true;
  private boolean failOnDuplicate;

  public IndexSymbols(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, SymbolIndex index) {
    this(projectInfo, compilationUnit, file, index, true);
  }

  public IndexSymbols(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, SymbolIndex index, boolean failOnDuplicate) {
    super(projectInfo, compilationUnit, file, false);
    this.index = index;
    this.failOnDuplicate = failOnDuplicate;
  }

  /**
   * Returns true if the verbose flag is set.
   *
   * @return true if verbose is set
   * @docgenVersion 9
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets the verbose flag.
   *
   * @param verbose the new value for the verbose flag
   * @return this object, for chaining
   * @docgenVersion 9
   */
  @Nonnull
  public IndexSymbols setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  /**
   * This is the endVisit method for the LambdaExpression class.
   * It takes in a node of type LambdaExpression and resolves the
   * method binding for that node.
   *
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull LambdaExpression node) {
    indexDef(node, node.resolveMethodBinding());
  }

  /**
   * This method overrides the endVisit method in order to index the definition of the given node.
   *
   * @param node    the node to be indexed
   * @param binding the binding associated with the node
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull MethodDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  /**
   * This method overrides the endVisit method in the ASTVisitor class.
   * It takes in a TypeDeclaration node as a parameter and uses the
   * resolveBinding() method to index the definition.
   *
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull TypeDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  /**
   * This method overrides the endVisit method in the ASTVisitor class.
   * It takes in a SingleVariableDeclaration node as a parameter and
   * resolves the binding for that node.
   *
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull SingleVariableDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  /**
   * This method overrides the endVisit method in the ASTVisitor class.
   * It is used to index the definition of a variable.
   *
   * @param node    the node being visited
   * @param binding the binding of the node
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull VariableDeclarationFragment node) {
    if (!(node.getParent() instanceof FieldDeclaration)) {
      indexDef(node, node.resolveBinding());
    }
  }

  /**
   * This method is called when the visitor encounters a field declaration.
   *
   * @param node the field declaration node
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull FieldDeclaration node) {
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

  /**
   * This is the endVisit method for QualifiedName nodes.
   *
   * @param node the QualifiedName node
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull QualifiedName node) {
    final ITypeBinding qualifierType = node.getQualifier().resolveTypeBinding();
    if (null != qualifierType && qualifierType.isArray()) return;
    final IBinding binding = node.resolveBinding();
    if (null != binding) {
      indexReference(node, binding);
    }
    super.endVisit(node);
  }

  /**
   * This method overrides the endVisit method in ASTVisitor.
   * If the parent of the node is not a QualifiedName,
   * the binding of the node is resolved and indexed.
   *
   * @param node - the node being visited
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull SimpleName node) {
    if (!(node.getParent() instanceof QualifiedName)) {
      final IBinding binding = node.resolveBinding();
      if (null != binding) {
        indexReference(node, binding);
      }
    }
    super.endVisit(node);
  }

  /**
   * Indexes a reference to the given node with the given binding.
   *
   * @param node    the node being referenced
   * @param binding the binding for the node, or null if none
   * @docgenVersion 9
   */
  public void indexReference(@Nonnull Name node, @Nullable IBinding binding) {
    if (null == binding) {
      if (isVerbose()) {
        assert false;
        debug(node, "Unresolved element for %s", binding.getName());
      }
      return;
    }
    SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(binding);
    if (null == bindingID) return;
    final SymbolIndex.ContextLocation contextLocation = getContextLocation(node);
    final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + getSpan(e.getValue())).reduce((a, b) -> a + "\n\t" + b).orElse("-");
    if (isVerbose())
      debug(node, "Reference to %s at %s within:\n\t%s", bindingID, contextLocation.location, contextPath);
    index.references.computeIfAbsent(bindingID, x -> new ArrayList<>()).add(node);
  }

  /**
   * Returns the context location for the given node.
   *
   * @param node the node to get the context location for
   * @return the context location for the given node
   * @docgenVersion 9
   */
  private SymbolIndex.ContextLocation getContextLocation(@Nonnull ASTNode node) {
    return new SymbolIndex.ContextLocation(getSpan(node), index.context(node));
  }

  /**
   * Indexes the definition of the given node.
   *
   * @param node    the node to index
   * @param binding the binding for the node, if any
   * @docgenVersion 9
   */
  private void indexDef(@Nonnull ASTNode node, @Nullable IBinding binding) {
    if (null == binding) return;
    final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(binding);
    if (null == bindingID) return;
    final SymbolIndex.ContextLocation contextLocation = getContextLocation(node);
    final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + getSpan(e.getValue())).reduce((a, b) -> a + "\n\t" + b).orElse("-");
    if (isVerbose()) debug(node, "Declaration of %s at %s within: \n\t%s", bindingID, getSpan(node), contextPath);
    final ASTNode replaced = index.definitions.put(bindingID, node);
    if (null != replaced) {
      if (failOnDuplicate)
        throw new RuntimeException(String.format("Duplicate declaration of %s in %s and %s", bindingID, getSpan(replaced), contextLocation.location));
      else
        warn(node, "Duplicate declaration of %s in %s and %s", bindingID, getSpan(replaced), contextLocation.location);
    }
  }

}
