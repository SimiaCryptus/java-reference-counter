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
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RefIgnore
public class RemoveRefs extends RefASTOperator {

  protected RemoveRefs(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  protected boolean isRefUtil(@NotNull MethodInvocation node) {
    final Expression expression = node.getExpression();
    if (expression instanceof SimpleName) {
      return ((SimpleName) expression).getFullyQualifiedName().equals("RefUtil");
    } else if (expression instanceof QualifiedName) {
      return ((QualifiedName) expression).getFullyQualifiedName().equals(RefUtil.class.getCanonicalName());
    } else {
      final IMethodBinding methodBinding = resolveMethodBinding(node);
      if (null == methodBinding) {
        warn(node, "Unresolved method binding %s", node);
        return false;
      } else {
        final String declaringClass = methodBinding.getDeclaringClass().getQualifiedName();
        return declaringClass.equals(RefUtil.class.getCanonicalName());
      }
    }
  }

  protected Expression unwrap(Expression subject) {
    if (subject instanceof ParenthesizedExpression) {
      return unwrap(((ParenthesizedExpression) subject).getExpression());
    } else if (subject instanceof CastExpression) {
      return unwrap(((CastExpression) subject).getExpression());
    } else {
      return subject;
    }
  }

  public static class ModifyMethodInvocation extends RemoveRefs {
    public ModifyMethodInvocation(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull final MethodInvocation node) {
      final String methodName = node.getName().toString();
      if (Arrays.asList("addRef", "freeRef", "addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
        Expression subject;
        if (Arrays.asList("addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
          subject = (Expression) copyIfAttached((ASTNode) node.arguments().get(0));
        } else {
          if (isRefUtil(node)) {
            subject = (Expression) copyIfAttached((ASTNode) node.arguments().get(0));
          } else {
            final Expression expression = node.getExpression();
            if (null == expression) {
              warn(node, "Naked method call. Cannot remove.");
              return;
            }
            subject = copyIfAttached(expression);
          }
        }
        info(node, "Removing %s and replacing with %s", methodName, subject);
        //        replace(node, subject);
        ASTNode parent = node.getParent();
        if (parent instanceof ConditionalExpression) {
          final ConditionalExpression conditionalExpression = (ConditionalExpression) parent;
          if (conditionalExpression.getThenExpression() instanceof NullLiteral) {
            if (conditionalExpression.getExpression() instanceof InfixExpression) {
              final InfixExpression infixExpression = (InfixExpression) conditionalExpression.getExpression();
              if (infixExpression.getLeftOperand().toString().equals(subject.toString())) {
                if (infixExpression.getRightOperand() instanceof NullLiteral) {
                  if (infixExpression.getOperator().equals(InfixExpression.Operator.EQUALS)) {
                    replace(conditionalExpression, subject);
                    parent = conditionalExpression.getParent();
                  }
                }
              }
            }
          }
        }
        if (parent instanceof MethodInvocation) {
          final List arguments = ((MethodInvocation) parent).arguments();
          final int index = arguments.indexOf(node);
          if (index < 0) {
            warn(node, "%s not found as argument to %s", node, ((MethodInvocation) parent).getName());
          } else {
            arguments.set(index, subject);
            info(node, "%s removed as argument %s of %s", methodName, index, parent);
          }
        } else if (parent instanceof ExpressionStatement) {
          subject = unwrap(subject);
          if (ASTUtil.isEvaluable(subject)) {
            info(subject, "%s replaced with %s", parent, subject);
            replace(parent, ast.newExpressionStatement(copyIfAttached(subject)));
          } else {
            info(subject, "%s removed", parent);
            delete((ExpressionStatement) parent);
          }
        } else if (parent instanceof ClassInstanceCreation) {
          final List arguments = ((ClassInstanceCreation) parent).arguments();
          final int index = arguments.indexOf(node);
          info(node, "%s removed as argument %s of %s", node, index, parent);
          arguments.set(index, subject);
        } else if (parent instanceof VariableDeclarationFragment) {
          info(node, "%s removed", node);
          ((VariableDeclarationFragment) parent).setInitializer(subject);
        } else if (parent instanceof Assignment) {
          info(node, "%s removed", node);
          ((Assignment) parent).setRightHandSide(subject);
        } else if (parent instanceof ArrayInitializer) {
          final List arguments = ((ArrayInitializer) parent).expressions();
          final int index = arguments.indexOf(node);
          arguments.set(index, subject);
          info(node, "%s removed as argument %s of %s", node, index, parent);
        } else {
          subject = unwrap(subject);
          info(subject, "%s replaced with %s", parent, subject);
          replace(node, copyIfAttached(subject));
        }
      }
    }
  }

  public static class ModifyBlock extends RemoveRefs {
    public ModifyBlock(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(Block node) {
      if (node.statements().isEmpty()) {
        final ASTNode parent = node.getParent();
        if (parent instanceof Initializer) {
          info(node, "delete %s", parent);
          parent.delete();
        }
      }
    }
  }
}
