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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * The InsertAddRefs class is used to insert additional references into a
 * document.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class InsertAddRefs extends RefASTOperator {

  protected InsertAddRefs(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * Adds references to the arguments of a given node.
   *
   * @param node          the node to add references to
   * @param arguments     the arguments to add references to
   * @param methodBinding the method binding for the node
   * @docgenVersion 9
   */
  public void addRefsToArguments(@Nonnull ASTNode node, @Nonnull List<ASTNode> arguments, @Nonnull IMethodBinding methodBinding) {
    String name = methodBinding.getReturnType().getQualifiedName();
    for (int i = 0; i < arguments.size(); i++) {
      ASTNode arg = arguments.get(i);
      if (consumesRefs(methodBinding, i) && shouldWrap(arg, name)) {
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
   * @return an Expression that represents a reference to the given ASTNode, or null if the node cannot be represented as a reference
   * @docgenVersion 9
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
   * Adds a reference to the given expression.
   *
   * @param expression the expression to add a reference to
   * @docgenVersion 9
   */
  protected void addRef(@Nonnull Expression expression) {
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
   * Returns true if the given expression is an instance accessor.
   *
   * @param expression the expression to check
   * @return true if the expression is an instance accessor, false otherwise
   * @docgenVersion 9
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
   * Returns true if the given expression should be added to the reference list, false otherwise.
   *
   * @param expression the expression to check
   * @return true if the expression should be added to the reference list, false otherwise
   * @docgenVersion 9
   */
  protected boolean shouldAddRef(Expression expression) {
    if (expression instanceof MethodInvocation) return false;
    if (expression instanceof ClassInstanceCreation) return false;
    if (expression instanceof CastExpression) return false;
    if (expression instanceof ArrayCreation) return false;
    if (expression instanceof ParenthesizedExpression)
      return shouldAddRef(((ParenthesizedExpression) expression).getExpression());
    return true;
  }

  /**
   * Returns true if the given ASTNode should be wrapped in a new node with the given name.
   *
   * @param arg  the ASTNode to check
   * @param name the name of the new node
   * @return true if the ASTNode should be wrapped, false otherwise
   * @docgenVersion 9
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
   * This class is used to modify an array initializer.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyArrayInitializer extends InsertAddRefs {

    public ModifyArrayInitializer(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the end of an array initializer is reached.
     *
     * @param node the node to visit
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull ArrayInitializer node) {
      if (skip(node)) return;
      final ITypeBinding typeBinding = resolveTypeBinding(node);
      if (null != typeBinding) {
        if (isRefCounted(node, typeBinding.getElementType())) {
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
   * This class is responsible for modifying a method invocation.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyMethodInvocation extends InsertAddRefs {

    public ModifyMethodInvocation(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the visitor encounters a method invocation node.
     *
     * @param node the method invocation node that was visited
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull MethodInvocation node) {
      final Expression expression = node.getExpression();
      Object[] args = new Object[]{null == expression ? "?" : expression.toString(), node.getName()};
      debug(node, "Processing method %s.%s", args);
      if (skip(node)) return;
      final IMethodBinding methodBinding = resolveMethodBinding(node);
      if (null == methodBinding) {
        warn(node, "Unresolved binding on %s", node);
        return;
      }
      addRefsToArguments(node, node.arguments(), methodBinding);
    }
  }

  /**
   * The ModifyAssignment class is used to modify an assignment.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyAssignment extends InsertAddRefs {

    public ModifyAssignment(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the end of an assignment is visited.
     *
     * @param node the node being visited
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull Assignment node) {
      final Expression expression = node.getRightHandSide();
      if (shouldAddRef(expression)) addRef(expression);
    }
  }

  /**
   * This class is responsible for modifying a variable declaration fragment.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyVariableDeclarationFragment extends InsertAddRefs {

    public ModifyVariableDeclarationFragment(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This is the endVisit method for the VariableDeclarationFragment class.
     * It overrides the endVisit method in the ASTVisitor class.
     *
     * @param node the VariableDeclarationFragment object
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull VariableDeclarationFragment node) {
      final Expression initializer = node.getInitializer();
      if (null != initializer && shouldAddRef(initializer)) addRef(initializer);
    }
  }

  /**
   * This class demonstrates how to modify a return statement.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyReturnStatement extends InsertAddRefs {

    public ModifyReturnStatement(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This is the endVisit method for the ReturnStatement class.
     * It overrides the endVisit method in the ASTVisitor class.
     *
     * @param node the ReturnStatement node to be visited
     * @return void
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull ReturnStatement node) {
      final Expression expression = node.getExpression();
      if (isInstanceAccessor(expression)) addRef(expression);
    }
  }

  /**
   * This class is used to modify constructor invocations.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyConstructorInvocation extends InsertAddRefs {

    public ModifyConstructorInvocation(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the visitor encounters a constructor invocation.
     *
     * @param node the constructor invocation node
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull ConstructorInvocation node) {
      if (skip(node)) return;
      final IMethodBinding methodBinding = resolveConstructorBinding(node);
      if (null == methodBinding) {
        warn(node, "Cannot resolve " + node);
        return;
      }
      addRefsToArguments(node, node.arguments(), methodBinding);
    }
  }

  /**
   * This class is used to modify an instance of a class.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyClassInstanceCreation extends InsertAddRefs {

    public ModifyClassInstanceCreation(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the visitor encounters a class instance creation expression.
     *
     * @param node the class instance creation expression to visit
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull ClassInstanceCreation node) {
      if (skip(node)) return;
      final IMethodBinding methodBinding = resolveConstructorBinding(node);
      if (null == methodBinding) {
        warn(node, "Cannot resolve %s", node);
        return;
      }
      if (node.arguments().size() > 0) {
        addRefsToArguments(node, node.arguments(), methodBinding);
      } else {
        debug(node, "No args %s", node);
      }
    }
  }
}
