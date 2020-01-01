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
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * The type Insert add refs.
 */
@RefIgnore
public class InsertAddRefs extends RefASTOperator {

  /**
   * Instantiates a new Insert add refs.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   */
  protected InsertAddRefs(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * Add refs to arguments.
   *
   * @param node      the node
   * @param arguments the arguments
   * @param name      the name
   */
  public void addRefsToArguments(@NotNull ASTNode node, @NotNull List<ASTNode> arguments, String name) {
    for (int i = 0; i < arguments.size(); i++) {
      ASTNode arg = arguments.get(i);
      if (shouldWrap(arg, name)) {
        final Expression expression = (Expression) arg;
        final ITypeBinding resolveTypeBinding = resolveTypeBinding(expression);
        if (null == resolveTypeBinding) {
          warn(arg, "Unresolved binding");
        } else if (isRefCounted(arg, resolveTypeBinding)) {
          arguments.set(i, wrapAddRef(expression, resolveTypeBinding));
          debug(node, "Argument addRef for %s: %s (%s) defined", name, resolveTypeBinding.getQualifiedName(), expression);
        } else {
          debug(node, "Non-refcounted arg %s in %s", expression, name);
        }
      }
    }
  }

  /**
   * Modify args boolean.
   *
   * @param declaringClass the declaring class
   * @return the boolean
   */
  public boolean modifyArgs(@NotNull ITypeBinding declaringClass) {
    return isRefAware(declaringClass);
  }

  /**
   * Wrap add ref expression.
   *
   * @param node the node
   * @return the expression
   */
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

  /**
   * Add ref.
   *
   * @param expression the expression
   */
  protected void addRef(@NotNull Expression expression) {
    final ITypeBinding resolveTypeBinding = resolveTypeBinding(expression);
    if (null == resolveTypeBinding) {
      warn(expression, "Unresolved binding for %s", expression);
    } else if (isRefCounted(expression, resolveTypeBinding)) {
      replace(expression, wrapAddRef(expression, resolveTypeBinding));
      debug(expression, "%s.addRef", expression);
    } else {
      debug(expression, "Non-refcounted %s", expression);
    }
  }

  /**
   * Is instance accessor boolean.
   *
   * @param expression the expression
   * @return the boolean
   */
  protected boolean isInstanceAccessor(Expression expression) {
    if (expression instanceof ThisExpression) return true;
    if (expression instanceof SimpleName) {
      if (ASTUtil.isField((SimpleName) expression)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Should add ref boolean.
   *
   * @param expression the expression
   * @return the boolean
   */
  protected boolean shouldAddRef(Expression expression) {
    if (expression instanceof MethodInvocation) return false;
    if (expression instanceof ClassInstanceCreation) return false;
    if (expression instanceof CastExpression) return false;
    if (expression instanceof ArrayCreation) return false;
    if (expression instanceof ParenthesizedExpression) return shouldAddRef(((ParenthesizedExpression) expression).getExpression());
    return true;
  }

  /**
   * Should wrap boolean.
   *
   * @param arg  the arg
   * @param name the name
   * @return the boolean
   */
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

  /**
   * The type Modify array initializer.
   */
  @RefIgnore
  public static class ModifyArrayInitializer extends InsertAddRefs {

    /**
     * Instantiates a new Modify array initializer.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
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
              debug(node, "Argument addRef for %s", next);
              expressions.set(i, methodInvocation);
            }
          }
        }
      }
    }
  }

  /**
   * The type Modify method invocation.
   */
  @RefIgnore
  public static class ModifyMethodInvocation extends InsertAddRefs {

    /**
     * Instantiates a new Modify method invocation.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
    public ModifyMethodInvocation(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull MethodInvocation node) {
      final Expression expression = node.getExpression();
      Object[] args = new Object[]{null == expression ? "?" : expression.toString(), node.getName()};
      debug(node, "Processing method %s.%s", args);
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
        debug(node, "Refcounted method %s", node);
        addRefsToArguments(node, node.arguments(), targetLabel);
      } else {
        debug(node, "Ignored method %s", targetLabel);
      }
    }
  }

  /**
   * The type Modify assignment.
   */
  @RefIgnore
  public static class ModifyAssignment extends InsertAddRefs {

    /**
     * Instantiates a new Modify assignment.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
    public ModifyAssignment(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull Assignment node) {
      final Expression expression = node.getRightHandSide();
      if (shouldAddRef(expression)) addRef(expression);
    }
  }

  /**
   * The type Modify variable declaration fragment.
   */
  @RefIgnore
  public static class ModifyVariableDeclarationFragment extends InsertAddRefs {

    /**
     * Instantiates a new Modify variable declaration fragment.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
    public ModifyVariableDeclarationFragment(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull VariableDeclarationFragment node) {
      final Expression initializer = node.getInitializer();
      if (null != initializer && shouldAddRef(initializer)) addRef(initializer);
    }
  }

  /**
   * The type Modify return statement.
   */
  @RefIgnore
  public static class ModifyReturnStatement extends InsertAddRefs {

    /**
     * Instantiates a new Modify return statement.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
    public ModifyReturnStatement(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull ReturnStatement node) {
      final Expression expression = node.getExpression();
      if (isInstanceAccessor(expression)) addRef(expression);
    }
  }

  /**
   * The type Modify constructor invocation.
   */
  @RefIgnore
  public static class ModifyConstructorInvocation extends InsertAddRefs {

    /**
     * Instantiates a new Modify constructor invocation.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
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
        debug(node, "Refcounted constructor %s", node);
        addRefsToArguments(node, node.arguments(), methodBinding.getReturnType().getQualifiedName());
      } else {
        debug(node, "Non-refcounted constructor %s", node);
      }
    }
  }

  /**
   * The type Modify class instance creation.
   */
  @RefIgnore
  public static class ModifyClassInstanceCreation extends InsertAddRefs {

    /**
     * Instantiates a new Modify class instance creation.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
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
        debug(node, "Refcounted constructor %s", node);
        if (node.arguments().size() > 0) {
          addRefsToArguments(node, node.arguments(), methodBinding.getReturnType().getQualifiedName());
        } else {
          debug(node, "No args %s", node);
        }
      } else {
        debug(node, "Non-refcounted constructor %s", node);
      }
    }
  }
}
