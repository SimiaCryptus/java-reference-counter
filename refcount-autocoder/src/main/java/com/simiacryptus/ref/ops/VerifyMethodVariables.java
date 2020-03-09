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

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.CollectableException;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RefIgnore
public class VerifyMethodVariables extends VerifyRefOperator {

  public VerifyMethodVariables(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Nullable
  public static ASTNode getExecutingMethod(@Nullable ASTNode node) {
    if (null == node) return null;
    if (node instanceof MethodDeclaration) return node;
    if (node instanceof EnhancedForStatement) return node;
    if (node instanceof LambdaExpression) return node;
    if (node instanceof Block) return node;
    if (node instanceof TypeDeclaration) return null;
    return getExecutingMethod(node.getParent());
  }

  @Override
  public void endVisit(@Nonnull MethodDeclaration node) {
    final IMethodBinding methodBinding = node.resolveBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    final List<SingleVariableDeclaration> parameters = node.parameters();
    ArrayList<CollectableException> exceptions = new ArrayList<>();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      try {
        IAnnotationBinding[] annotations = methodBinding.getParameterAnnotations(i);
        ITypeBinding type = methodBinding.getParameterTypes()[i];
        if (!ASTUtil.findAnnotation(RefIgnore.class, annotations).isPresent()) {
          if (ASTUtil.findAnnotation(RefAware.class, annotations).isPresent() || isRefCounted(node, type)) {
            Block body = node.getBody();
            SimpleName name = parameters.get(i).getName();
            if (null == body) continue;
            processScope(body, name, TerminalState.Freed);
          }
        }
      } catch (CollectableException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
  }

  public void processScope(Block body, SimpleName name, TerminalState requiredTermination) {
    ReferenceState finalState = processStatement(body, name, new ReferenceState(name, null, null), new ArrayList<>(), requiredTermination);
    if (!requiredTermination.validate(finalState)) {
      fatal(body, "Reference for %s ends in state %s", name, finalState);
    }
  }

  @Override
  public void endVisit(@Nonnull SingleVariableDeclaration node) {
    ArrayList<CollectableException> exceptions = new ArrayList<>();
    try {
      if (getLocation(node).equals("BasicTrainable.java:195")) {
        info(node, "Processing %s", node);
      }
      IVariableBinding binding = node.resolveBinding();
      if (null == binding) {
        warn(node, "Unresolved type");
      } else {
        if (!ASTUtil.hasAnnotation(binding, RefIgnore.class)) {
          if (isRefCounted(node, binding.getType()) || ASTUtil.hasAnnotation(binding, RefAware.class)) {
            processStatements(node);
          }
        }
      }
    } catch (CollectableException e) {
      exceptions.add(e);
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
    super.endVisit(node);
  }

  @Override
  public void endVisit(@Nonnull VariableDeclarationFragment node) {
    IVariableBinding binding = node.resolveBinding();
    if (null == binding) {
      warn(node, "Unresolved type");
      return;
    }
    if (!ASTUtil.hasAnnotation(binding, RefIgnore.class)) {
      if (isRefCounted(node, binding.getType()) || ASTUtil.hasAnnotation(binding, RefAware.class)) {
        processStatements(node);
      }
    }
  }

  @NotNull
  public void processStatements(VariableDeclaration node) {
    ASTNode astNode = getExecutingMethod(node);
    if (astNode == null) {
      info(node, "Lambda nor Method not found for %s", node);
      return;
    }
    if (astNode instanceof LambdaExpression) {
      LambdaExpression lambdaExpression = (LambdaExpression) astNode;
      ASTNode body = lambdaExpression.getBody();
      processBlock(body, TerminalState.Freed, node.getName());
    } else if (astNode instanceof MethodDeclaration) {
      MethodDeclaration methodDeclaration = (MethodDeclaration) astNode;
      ASTNode body = methodDeclaration.getBody();
      if (body != null) processBlock(body, TerminalState.Freed, node.getName());
    } else if (astNode instanceof EnhancedForStatement) {
      processBlock(((EnhancedForStatement) astNode).getBody(), TerminalState.Any, node.getName());
    } else {
      Block body = (Block) astNode;
      if (null != body) {
        SimpleName name = node.getName();
        if (body == null) fatal(name, "Block not found");
        List<Statement> statements = body.statements();
        if (null != statements) {
          Statement statement = getStatement(node);
          int indexOf = statements.indexOf(statement);
          if (indexOf >= 0) statements = statements.subList(indexOf + 1, statements.size());
          processScope(statements, name, TerminalState.Freed);
        }
      }
    }
  }

  public void processBlock(ASTNode block, TerminalState requiredTermination, SimpleName name) {
    if (block == null) {
      fatal(name, "Block not found");
    }
    if (block instanceof Block) {
      List<Statement> statements = ((Block) block).statements();
      if (null != statements) processScope(statements, name, requiredTermination);
    } else {
      ReferenceState startState = new ReferenceState(name, null, null);
      ReferenceState endState = processNode(block, name, startState, new ArrayList<>(), requiredTermination);
      if (!requiredTermination.validate(endState)) {
        fatal(block, "Invalid state for %s: %s", name, endState);
      }
    }
  }

  public void processScope(List<Statement> statements, SimpleName name, TerminalState requiredTermination) {
    ArrayList<Statement> finalizers = new ArrayList<>();
    ReferenceState state = new ReferenceState(name, null, null);
    for (Statement statement : statements) {
      state = processStatement(statement, name, state, finalizers, requiredTermination);
    }
    if (!requiredTermination.validate(state)) {
      fatal(statements.isEmpty() ? name : statements.get(statements.size() - 1), "Reference for %s ends in state %s", name, state);
    }
  }

}
