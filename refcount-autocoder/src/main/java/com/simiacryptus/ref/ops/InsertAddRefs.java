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

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

@RefIgnore
public class InsertAddRefs extends RefASTOperator {

  protected InsertAddRefs(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  public void addRefsToArguments(ASTNode node, @NotNull List<ASTNode> arguments, String name) {
    for (int i = 0; i < arguments.size(); i++) {
      ASTNode arg = arguments.get(i);
      if (shouldWrap(arg, name)) {
        final Expression expression = (Expression) arg;
        final ITypeBinding resolveTypeBinding = resolveTypeBinding(expression);
        if (null == resolveTypeBinding) {
          warn(arg, "Unresolved binding");
          return;
        }
        if (isRefCounted(arg, resolveTypeBinding)) {
          arguments.set(i, wrapAddRef(expression, resolveTypeBinding));
          info(node, "Argument addRef for %s: %s (%s) defined", name, resolveTypeBinding.getQualifiedName(), expression);
        } else {
          info(node, "Non-refcounted arg %s in %s", expression, name);
        }
      }
    }
  }

  public boolean modifyArgs(@NotNull ITypeBinding declaringClass) {
    return isRefAware(declaringClass);
  }

  @Nullable
  public Expression wrapAddRef(ASTNode node) {
    if (node instanceof SimpleName) {
      final SimpleName name = (SimpleName) node;
      if (ASTUtil.derives(resolveTypeBinding(name), ReferenceCounting.class)) {
        return wrapAddRef(name, resolveTypeBinding(name));
      }
    }
    return null;
  }

  protected void addRef(Expression expression) {
    final ITypeBinding resolveTypeBinding = resolveTypeBinding(expression);
    if (null == resolveTypeBinding) {
      warn(expression, "Unresolved binding for %s", expression);
      return;
    }
    if (isRefCounted(expression, resolveTypeBinding)) {
      replace(expression, wrapAddRef(expression, resolveTypeBinding));
      info(expression, "%s.addRef", expression);
    } else {
      info(expression, "Non-refcounted %s", expression);
    }
  }

  protected boolean isInstanceAccessor(Expression expression) {
    if (expression instanceof ThisExpression) return true;
    if (expression instanceof SimpleName) {
      if (ASTUtil.isField((SimpleName) expression)) {
        return true;
      }
    }
    return false;
  }

  protected boolean shouldAddRef(Expression expression) {
    if (expression instanceof MethodInvocation) return false;
    if (expression instanceof ClassInstanceCreation) return false;
    if (expression instanceof CastExpression) return false;
    if (expression instanceof ArrayCreation) return false;
    if (expression instanceof ParenthesizedExpression) return shouldAddRef(((ParenthesizedExpression) expression).getExpression());
    return true;
  }

  protected boolean shouldWrap(ASTNode arg, String name) {
    if (arg instanceof ClassInstanceCreation) {
      debug(arg, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      return false;
    } else if (arg instanceof AnonymousClassDeclaration) {
      debug(arg, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      return false;
    } else if (arg instanceof LambdaExpression) {
      debug(arg, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      return false;
    } else if (arg instanceof MethodInvocation) {
      debug(arg, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      return false;
    } else if (arg instanceof ArrayCreation) {
      debug(arg, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      return false;
    } else if (arg instanceof CastExpression) {
      return shouldWrap(((CastExpression) arg).getExpression(), name);
    } else if (arg instanceof ParenthesizedExpression) {
      return shouldWrap(((ParenthesizedExpression) arg).getExpression(), name);
    } else if (arg instanceof Expression) {
      return true;
    } else {
      warn(arg, "Unexpected type %s in %s", arg.getClass().getSimpleName(), name);
      return false;
    }
  }

  public static class ModifyArrayInitializer extends InsertAddRefs {

    public ModifyArrayInitializer(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull ArrayInitializer node) {
      if (skip(node)) return;
      final ITypeBinding typeBinding = resolveTypeBinding(node);
      if (null != typeBinding) {
        if (modifyArgs(typeBinding.getElementType())) {
          final List expressions = node.expressions();
          for (int i = 0; i < expressions.size(); i++) {
            Object next = expressions.get(i);
            Expression methodInvocation = wrapAddRef((ASTNode) next);
            if (null != methodInvocation) {
              info(node, "Argument addRef for %s", next);
              expressions.set(i, methodInvocation);
            }
          }
        }
      }
    }
  }

  public static class ModifyMethodInvocation extends InsertAddRefs {

    public ModifyMethodInvocation(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull MethodInvocation node) {
      final Expression expression = node.getExpression();
      info(node, "Processing method %s.%s", null == expression ? "?" : expression.toString(), node.getName());
      if (skip(node)) return;
      final IMethodBinding methodBinding = resolveMethodBinding(node);
      if (null == methodBinding) {
        warn(node, "Unresolved binding on %s", node);
        return;
      }
      final String targetLabel;
      if (null != expression && expression instanceof Name) {
        targetLabel = expression.toString() + "." + node.getName();
      } else {
        targetLabel = methodBinding.getDeclaringClass().getQualifiedName() + "::" + node.getName();
      }
      if (consumesRefs(methodBinding, null == expression ? null : resolveTypeBinding(expression))) {
        info(node, "Refcounted method %s", node);
        addRefsToArguments(node, node.arguments(), targetLabel);
      } else {
        info(node, "Ignored method %s", targetLabel);
      }
    }
  }

  public static class ModifyAssignment extends InsertAddRefs {

    public ModifyAssignment(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(Assignment node) {
      final Expression expression = node.getRightHandSide();
      if (shouldAddRef(expression)) addRef(expression);
    }
  }

  public static class ModifyVariableDeclarationFragment extends InsertAddRefs {

    public ModifyVariableDeclarationFragment(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(VariableDeclarationFragment node) {
      final Expression initializer = node.getInitializer();
      if (null != initializer && shouldAddRef(initializer)) addRef(initializer);
    }
  }

  public static class ModifyReturnStatement extends InsertAddRefs {

    public ModifyReturnStatement(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(ReturnStatement node) {
      final Expression expression = node.getExpression();
      if (isInstanceAccessor(expression)) addRef(expression);
    }
  }

  public static class ModifyConstructorInvocation extends InsertAddRefs {

    public ModifyConstructorInvocation(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull ConstructorInvocation node) {
      if (skip(node)) return;
      final IMethodBinding methodBinding = resolveConstructorBinding(node);
      if (null == methodBinding) {
        warn(node, "Cannot resolve " + node);
        return;
      }
      if (consumesRefs(methodBinding, methodBinding.getReturnType()) && node.arguments().size() > 0) {
        info(node, "Refcounted constructor %s", node);
        addRefsToArguments(node, node.arguments(), methodBinding.getReturnType().getQualifiedName());
      } else {
        info(node, "Non-refcounted constructor %s", node);
      }
    }
  }

  public static class ModifyClassInstanceCreation extends InsertAddRefs {

    public ModifyClassInstanceCreation(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull ClassInstanceCreation node) {
      if (skip(node)) return;
      final IMethodBinding methodBinding = resolveConstructorBinding(node);
      if (null == methodBinding) {
        warn(node, "Cannot resolve %s", node);
        return;
      }
      if (consumesRefs(methodBinding, resolveTypeBinding(node))) {
        info(node, "Refcounted constructor %s", node);
        if (node.arguments().size() > 0) {
          addRefsToArguments(node, node.arguments(), methodBinding.getReturnType().getQualifiedName());
        } else {
          debug(node, "No args %s", node);
        }
      } else {
        info(node, "Non-refcounted constructor %s", node);
      }
    }
  }
}
