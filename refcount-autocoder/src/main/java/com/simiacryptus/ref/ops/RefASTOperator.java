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
import com.simiacryptus.ref.core.ops.ASTOperator;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * The type Ref ast operator.
 */
@RefIgnore
abstract class RefASTOperator extends ASTOperator {
  @NotNull
  private static Map<File, Integer> fileCounters = new HashMap<>();
  @NotNull
  private static Map<File, AtomicInteger> identifierCounters = new HashMap<>();
  @NotNull
  private String tempVarPrefix = "temp";

  /**
   * Instantiates a new Ref ast operator.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   */
  public RefASTOperator(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * Gets add ref method.
   *
   * @param type the type
   * @return the add ref method
   */
  @Nullable
  public static IMethodBinding getAddRefMethod(@NotNull ITypeBinding type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(method ->
        method.getName().equals("addRef")
            && (0 != (method.getModifiers() & Modifier.PUBLIC))
            && method.getParameterTypes().length == 0
    ).findAny().orElse(null);
  }

  /**
   * Gets add refs method.
   *
   * @param type the type
   * @return the add refs method
   */
  @Nullable
  public static IMethodBinding getAddRefsMethod(@NotNull ITypeBinding type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(method ->
        method.getName().equals("addRefs")
            && (0 != (method.getModifiers() & Modifier.PUBLIC))
            && (0 != (method.getModifiers() & Modifier.STATIC))
            && method.getParameterTypes().length == 1
    ).findAny().orElse(null);
  }

  @Override
  public boolean preVisit2(ASTNode node) {
    if (node instanceof TypeDeclaration) {
      final ITypeBinding binding = ASTOperator.resolveBinding((TypeDeclaration) node);
      if (null == binding) {
        warn(node, "Unresolved binding");
        return false;
      }
      if (ASTUtil.hasAnnotation(binding, RefIgnore.class)) {
        warn(node, "Marked with RefIgnore");
        return false;
      }
    }
    return super.preVisit2(node);
  }

  /**
   * Gets method declaration.
   *
   * @param node the node
   * @return the method declaration
   */
  @Nullable
  @SuppressWarnings("unused")
  public MethodDeclaration getMethodDeclaration(ASTNode node) {
    if (node instanceof MethodDeclaration) return (MethodDeclaration) node;
    if (node instanceof Statement) return null;
    if (node instanceof TypeDeclaration) return null;
    final ASTNode parent = node.getParent();
    if (null != parent) return getMethodDeclaration(parent);
    return null;
  }

  /**
   * Consumes refs boolean.
   *
   * @param methodBinding the method binding
   * @param expression    the expression
   * @return the boolean
   */
  protected final boolean consumesRefs(@Nonnull IMethodBinding methodBinding, @Nullable ITypeBinding expression) {
    final String methodName = methodBinding.getName();
    if (ASTUtil.hasAnnotation(methodBinding, RefIgnore.class)) return false;
    if (methodName.equals("addRefs")) {
      return false;
    }
    if (methodName.equals("freeRefs")) {
      return false;
    }
    return null == expression ? isRefAware(methodBinding.getDeclaringClass()) : isRefAware(expression);
  }

  /**
   * Free ref statement statement.
   *
   * @param node        the node
   * @param typeBinding the type binding
   * @return the statement
   */
  @NotNull
  protected final Statement freeRefStatement(@NotNull ASTNode node, @Nullable ITypeBinding typeBinding) {
    if (null == typeBinding) {
      warn(1, node, "Cannot add freeRef (binding not resolved)");
      return null;
    }
    final IfStatement ifStatement = ast.newIfStatement();
    final InfixExpression infixExpression = ast.newInfixExpression();
    infixExpression.setLeftOperand(ast.newNullLiteral());
    infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
    infixExpression.setRightOperand(copyIfAttached((Expression) node));
    ifStatement.setExpression(infixExpression);
    ifStatement.setThenStatement(newFreeRef(node, typeBinding));
    debug(1, node, "Added freeRef");
    return ifStatement;
  }

  /**
   * Free ref util statement statement.
   *
   * @param node        the node
   * @param typeBinding the type binding
   * @return the statement
   */
  @NotNull
  protected final Statement freeRefUtilStatement(@NotNull ASTNode node, @NotNull ITypeBinding typeBinding) {
    return ast.newExpressionStatement(newFreeRefUtil(node, typeBinding));
  }

  /**
   * Gets temp identifier.
   *
   * @param node the node
   * @return the temp identifier
   */
  protected final String getTempIdentifier(@NotNull ASTNode node) {
    final String id = String.format(tempVarPrefix + "_%02d_%04d",
        (long) fileCounters.computeIfAbsent(file, x -> fileCounters.size()),
        (long) identifierCounters.computeIfAbsent(file, x -> new AtomicInteger(0)).incrementAndGet());
    debug(1, node, "Creating %s", id);
    return id;
  }

  /**
   * Gets type binding.
   *
   * @param declaration the declaration
   * @return the type binding
   */
  @Nullable
  protected final ITypeBinding getTypeBinding(@NotNull VariableDeclaration declaration) {
    final IVariableBinding iVariableBinding = resolveBinding(declaration);
    if (iVariableBinding == null) {
      warn(1, declaration, "Cannot resolve method of %s", declaration);
      return null;
    }
    return iVariableBinding.getType();
  }

  /**
   * Is ref aware boolean.
   *
   * @param declaringClass the declaring class
   * @return the boolean
   */
  protected final boolean isRefAware(@NotNull ITypeBinding declaringClass) {
    if (declaringClass.getTypeDeclaration().getQualifiedName().equals(Map.Entry.class.getCanonicalName())) return true;
    return ASTUtil.hasAnnotation(declaringClass, RefAware.class);
  }

  /**
   * Is ref counted boolean.
   *
   * @param node the node
   * @param type the type
   * @return the boolean
   */
  protected final boolean isRefCounted(ASTNode node, @NotNull ITypeBinding type) {
    if (node instanceof MethodReference) return false;
    if (type.isPrimitive()) return false;
    if (type.getTypeDeclaration().getQualifiedName().equals(Optional.class.getCanonicalName())) {
      final ITypeBinding[] typeArguments = type.getTypeArguments();
      if (null == typeArguments || 0 == typeArguments.length) {
        warn(1, node, "No type argument for Optional");
      }
      return isRefCounted(node, typeArguments[0]);
    }
    if (type.isArray()) {
      type = type.getElementType();
    }
    if (ASTUtil.derives(type, ReferenceCounting.class)) {
      debug(1, node, "Derives ReferenceCounting: %s (%s)", node, type.getQualifiedName());
      return true;
    }
    if (ASTUtil.derives(type, Map.Entry.class)) {
      debug(1, node, "Derives Map.Entry: %s (%s)", node, type.getQualifiedName());
      return true;
    }
//    if (type.isInterface() && !(node instanceof LambdaExpression)) {
//      info(1, node, "Is potentially refcounted interface: %s (%s)", node, type.getQualifiedName());
//      return true;
//    }
//    if (isRefAware(typeBinding)) {
//      info(1, node, "Marked with @RefAware: %s (%s)", node, type);
//      return true;
//    }
    debug(node, "Not refcounted: %s (%s)", node, type.getQualifiedName());
    return false;
  }

  /**
   * Is temp identifier boolean.
   *
   * @param name the name
   * @return the boolean
   */
  protected final boolean isTempIdentifier(@NotNull SimpleName name) {
    return Pattern.matches(tempVarPrefix + "[\\d_]+", name.toString());
  }

  /**
   * Method consumes self refs boolean.
   *
   * @param methodBinding the method binding
   * @return the boolean
   */
  protected final boolean methodConsumesSelfRefs(@Nonnull IMethodBinding methodBinding) {
    final String qualifiedName = methodBinding.getDeclaringClass().getTypeDeclaration().getQualifiedName();
    final String methodName = methodBinding.getName();
    if (qualifiedName.equals(Optional.class.getCanonicalName())) {
      if (methodName.equals("get")) {
        return true;
      }
    }
    return false;
  }

  /**
   * New add ref method invocation.
   *
   * @param name        the name
   * @param typeBinding the type binding
   * @return the method invocation
   */
  @NotNull
  protected final MethodInvocation newAddRef(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(copyIfAttached(name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression(copyIfAttached((Expression) name));
      return methodInvocation;
    }
  }

  /**
   * New free ref statement.
   *
   * @param node        the node
   * @param typeBinding the type binding
   * @return the statement
   */
  @NotNull
  protected final Statement newFreeRef(@NotNull ASTNode node, @NotNull ITypeBinding typeBinding) {
    if (typeBinding.isArray()) {
      if (ASTUtil.derives(typeBinding.getElementType(), ReferenceCounting.class)) {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("freeRefs"));
        methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, ReferenceCounting.class));
        methodInvocation.arguments().add(copyIfAttached(node));
        return ast.newExpressionStatement(methodInvocation);
      } else {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("freeRefs"));
        methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, RefUtil.class));
        methodInvocation.arguments().add(copyIfAttached(node));
        return ast.newExpressionStatement(methodInvocation);
      }
    } else if (ASTUtil.derives(typeBinding, ReferenceCounting.class)) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression(copyIfAttached((Expression) node));
      return ast.newExpressionStatement(methodInvocation);
    } else {
      final MethodInvocation freeInvocation = ast.newMethodInvocation();
      freeInvocation.setName(ast.newSimpleName("freeRef"));
      freeInvocation.setExpression(ASTUtil.newQualifiedName(ast, RefUtil.class));
      freeInvocation.arguments().add(copyIfAttached((Expression) node));
      return ast.newExpressionStatement(freeInvocation);
    }
  }

  /**
   * New free ref util method invocation.
   *
   * @param name        the name
   * @param typeBinding the type binding
   * @return the method invocation
   */
  @NotNull
  protected final MethodInvocation newFreeRefUtil(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    if (typeBinding.isArray()) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRefs"));
      methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, ReferenceCounting.class));
      methodInvocation.arguments().add(copyIfAttached(name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, RefUtil.class));
      methodInvocation.arguments().add(copyIfAttached(name));
      return methodInvocation;
    }
  }

  /**
   * Skip boolean.
   *
   * @param node the node
   * @return the boolean
   */
  protected final boolean skip(@NotNull ASTNode node) {
    return enclosingMethods(node).stream().anyMatch(enclosingMethod -> {
      if (ASTUtil.hasAnnotation(enclosingMethod, RefIgnore.class)) return true;
      final String methodName = enclosingMethod.getName();
      return methodName.equals("addRefs") || methodName.equals("freeRefs");
    });
  }

  /**
   * Wrap add ref expression.
   *
   * @param expression the expression
   * @param type       the type
   * @return the expression
   */
  @NotNull
  protected final Expression wrapAddRef(@NotNull Expression expression, @NotNull ITypeBinding type) {
    if (expression instanceof CastExpression) {
      final ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
      replace(expression, parenthesizedExpression);
      parenthesizedExpression.setExpression(expression);
      return wrapAddRef(parenthesizedExpression, type);
    }
    if (null == type) {
      warn(1, expression, "No type for %s; cannot addRef", expression);
      return copyIfAttached(expression);
    } else if (type.isArray()) {
      if (null == getAddRefsMethod(type.getElementType())) {
        warn(1, expression, "No AddRefs method defined for %s", expression);
        return copyIfAttached(expression);
      } else {
        final String qualifiedName = type.getElementType().getQualifiedName();
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRefs"));
        methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, qualifiedName.split("<")[0].split("\\.")));
        methodInvocation.arguments().add(copyIfAttached(expression));
        debug(1, expression, "AddRef for %s", expression);
        return methodInvocation;
      }
    } else {
      if (null == getAddRefMethod(type)) {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRef"));
        methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, RefUtil.class));
        methodInvocation.arguments().add(copyIfAttached(expression));
        debug(1, expression, "AddRef for %s", expression);
        return methodInvocation;
      } else {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRef"));
        methodInvocation.setExpression(copyIfAttached(expression));
        debug(1, expression, "AddRef for %s", expression);
        if (expression instanceof SimpleName) {
          final InfixExpression infixExpression = ast.newInfixExpression();
          infixExpression.setLeftOperand(copyIfAttached(expression));
          infixExpression.setOperator(InfixExpression.Operator.EQUALS);
          infixExpression.setRightOperand(ast.newNullLiteral());
          final ConditionalExpression conditionalExpression = ast.newConditionalExpression();
          conditionalExpression.setExpression(infixExpression);
          conditionalExpression.setElseExpression(methodInvocation);
          conditionalExpression.setThenExpression(ast.newNullLiteral());
          return conditionalExpression;
        } else {
          return methodInvocation;
        }
      }
    }
  }

}
