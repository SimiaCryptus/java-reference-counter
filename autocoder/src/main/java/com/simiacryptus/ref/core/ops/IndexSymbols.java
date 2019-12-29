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

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class IndexSymbols extends ASTScanner {

  public final SymbolIndex index;
  private boolean verbose = true;
  private boolean failOnDuplicate;

  public IndexSymbols(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, SymbolIndex index) {
    this(projectInfo, compilationUnit, file, index, true);
  }

  public IndexSymbols(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, SymbolIndex index, boolean failOnDuplicate) {
    super(projectInfo, compilationUnit, file, false);
    this.index = index;
    this.failOnDuplicate = failOnDuplicate;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public IndexSymbols setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  public LinkedHashMap<SymbolIndex.BindingID, SymbolIndex.Span> context(ASTNode node) {
    final LinkedHashMap<SymbolIndex.BindingID, SymbolIndex.Span> list = new LinkedHashMap<>();
    final ASTNode parent = node.getParent();
    if (parent != null) list.putAll(context(parent));
    if (node instanceof MethodDeclaration) {
      final MethodDeclaration methodDeclaration = (MethodDeclaration) node;
      list.put(index.getBindingID(methodDeclaration.resolveBinding()), getSpan(node));
    } else if (node instanceof LambdaExpression) {
      final LambdaExpression lambdaExpression = (LambdaExpression) node;
      final IMethodBinding methodBinding = lambdaExpression.resolveMethodBinding();
      if (methodBinding == null) {
        warn(node, "Unresolved binding for %s", node);
      } else {
        list.put(index.getBindingID(methodBinding).setType("Lambda"), getSpan(node));
      }
    } else if (node instanceof TypeDeclaration) {
      final TypeDeclaration typeDeclaration = (TypeDeclaration) node;
      final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
      if (typeBinding == null) {
        warn(node, "Unresolved binding for %s", node);
      } else {
        list.put(index.getBindingID(typeBinding), getSpan(node));
      }
    }
    return list;
  }

  @Override
  public void endVisit(LambdaExpression node) {
    indexDef(node, node.resolveMethodBinding());
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  @Override
  public void endVisit(SingleVariableDeclaration node) {
    indexDef(node, node.resolveBinding());
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    if (!(node.getParent() instanceof FieldDeclaration)) {
      indexDef(node, node.resolveBinding());
    }
  }

  @Override
  public void endVisit(FieldDeclaration node) {
    final List fragments = node.fragments();
    for (Object fragment : fragments) {
      if (fragment instanceof VariableDeclarationFragment) {
        final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
        indexDef(node, variableDeclarationFragment.resolveBinding());
      } else {
        if (isVerbose()) info(node, "Other fragment type %s", fragment.getClass().getSimpleName());
      }
    }
  }

  @Override
  public void endVisit(QualifiedName node) {
    final ITypeBinding qualifierType = node.getQualifier().resolveTypeBinding();
    if (null != qualifierType && qualifierType.isArray()) return;
    final IBinding binding = node.resolveBinding();
    if (null != binding) {
      indexReference(node, binding);
    }
    super.endVisit(node);
  }

  @Override
  public void endVisit(SimpleName node) {
    if (!(node.getParent() instanceof QualifiedName)) {
      final IBinding binding = node.resolveBinding();
      if (null != binding) {
        indexReference(node, binding);
      }
    }
    super.endVisit(node);
  }

  public void indexReference(Name node, IBinding binding) {
    if (null != binding) {
      SymbolIndex.BindingID bindingID = index.getBindingID(binding);
      if (null != bindingID) {
        final SymbolIndex.ContextLocation contextLocation = getContextLocation(node);
        final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
        if (isVerbose()) info(node, "Reference to %s at %s within:\n\t%s", bindingID, contextLocation.location, contextPath);
        index.references.computeIfAbsent(bindingID, x -> new ArrayList<>()).add(contextLocation);
      }
    } else {
      if (isVerbose()) info(node, "Unresolved element for %s", binding.getName());
    }
  }

  @NotNull
  protected SymbolIndex.ContextLocation getContextLocation(ASTNode node) {
    return new SymbolIndex.ContextLocation(getSpan(node), context(node));
  }

  private void indexDef(ASTNode node, IBinding binding) {
    if (null == binding) return;
    final SymbolIndex.ContextLocation contextLocation = getContextLocation(node);
    final SymbolIndex.BindingID bindingID = index.getBindingID(binding);
    final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
    if (isVerbose()) info(node, "Declaration of %s at %s within: \n\t%s", bindingID, getSpan(node), contextPath);
    final SymbolIndex.ContextLocation replaced = index.definitionLocations.put(bindingID, contextLocation);
    if (null != replaced) {
      if (failOnDuplicate) throw new RuntimeException(String.format("Duplicate declaration of %s in %s and %s", bindingID, replaced.location, contextLocation.location));
      warn(node, "Duplicate declaration of %s in %s and %s", bindingID, replaced.location, contextLocation.location);
    }
    index.definitionNodes.put(bindingID, node);
  }

}
