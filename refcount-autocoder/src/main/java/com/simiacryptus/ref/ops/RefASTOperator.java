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

@RefIgnore
abstract class RefASTOperator extends ASTOperator {
  @NotNull
  private static Map<File, Integer> fileCounters = new HashMap<>();
  @NotNull
  private static Map<File, AtomicInteger> identifierCounters = new HashMap<>();
  @NotNull
  private String tempVarPrefix = "temp";

  public RefASTOperator(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Nullable
  public static IMethodBinding getAddRefMethod(@NotNull ITypeBinding type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(method ->
        method.getName().equals("addRef")
            && (0 != (method.getModifiers() & Modifier.PUBLIC))
            && method.getParameterTypes().length == 0
    ).findAny().orElse(null);
  }

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
  public final boolean preVisit2(ASTNode node) {
    if (node instanceof TypeDeclaration) {
      final ITypeBinding binding = ASTOperator.resolveBinding((TypeDeclaration) node);
      if (null == binding) {
        warn(node, "Unresolved binding");
        return false;
      }
      if (skip(node, binding)) return false;
    } else if (node instanceof MethodDeclaration) {
      final @Nullable IMethodBinding binding = resolveBinding((MethodDeclaration) node);
      if (null == binding) {
        warn(node, "Unresolved binding");
        return false;
      }
      if (skip(node, binding)) return false;
    }
    return super.preVisit2(node);
  }

  @Nullable
  @SuppressWarnings("unused")
  public MethodDeclaration getMethodDeclaration(ASTNode node) {
    if (null == node) return null;
    if (node instanceof MethodDeclaration) return (MethodDeclaration) node;
    if (node instanceof Statement) return null;
    if (node instanceof TypeDeclaration) return null;
    return getMethodDeclaration(node.getParent());
  }

  @Nullable
  @SuppressWarnings("unused")
  public Statement getStatement(ASTNode node) {
    if (null == node) return null;
    if (node instanceof Statement) return (Statement) node;
    if (node instanceof MethodDeclaration) return null;
    if (node instanceof TypeDeclaration) return null;
    return getStatement(node.getParent());
  }

  protected boolean skip(ASTNode node, IBinding binding) {
    if (ASTUtil.hasAnnotation(binding, RefIgnore.class)) {
      debug(node, "Marked with RefIgnore");
      return true;
    }
    return false;
  }

  protected final boolean consumesRefs(@Nonnull IMethodBinding methodBinding, int index) {
    if (ASTUtil.hasAnnotation(methodBinding, RefIgnore.class)) return false;
    if (methodBinding.getName().equals("addRefs")) {
      return false;
    }
    if (methodBinding.getName().equals("freeRefs")) {
      return false;
    }
    ITypeBinding expression = methodBinding.getReturnType();
    if (null == expression) expression = methodBinding.getDeclaringClass();
    if (expression.getTypeDeclaration().getQualifiedName().equals(Map.Entry.class.getCanonicalName())) return true;
    return ASTUtil.findAnnotation(RefAware.class, methodBinding.getParameterAnnotations(index)).isPresent();
  }

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

  @NotNull
  protected final Statement freeRefUtilStatement(@NotNull ASTNode node, @NotNull ITypeBinding typeBinding) {
    return ast.newExpressionStatement(newFreeRefUtil(node, typeBinding));
  }

  protected final String getTempIdentifier(@NotNull ASTNode node) {
    final String id = String.format(tempVarPrefix + "_%02d_%04d",
        (long) fileCounters.computeIfAbsent(file, x -> fileCounters.size()),
        (long) identifierCounters.computeIfAbsent(file, x -> new AtomicInteger(0)).incrementAndGet());
    debug(1, node, "Creating %s", id);
    return id;
  }

  @Nullable
  protected final ITypeBinding getTypeBinding(@NotNull VariableDeclaration declaration) {
    final IVariableBinding iVariableBinding = resolveBinding(declaration);
    if (iVariableBinding == null) {
      warn(1, declaration, "Cannot resolve method of %s", declaration);
      return null;
    }
    return iVariableBinding.getType();
  }

  protected final boolean isRefCounted(ASTNode node, @NotNull ITypeBinding type) {
    if (type.isPrimitive()) return false;
    if (type.isArray()) {
      return isRefCounted(node, type.getElementType());
    }
    if (ASTUtil.derives(type, ReferenceCounting.class)) {
      debug(1, node, "Derives ReferenceCounting: %s", type.getQualifiedName());
      return true;
    }
    if (type.getTypeDeclaration().getQualifiedName().equals(Optional.class.getCanonicalName())) {
      final ITypeBinding[] typeArguments = type.getTypeArguments();
      if (null == typeArguments || 0 == typeArguments.length) {
        warn(1, node, "No type argument for Optional");
        return false;
      } else {
        return isRefCounted(node, typeArguments[0]);
      }
    }
    if (ASTUtil.derives(type, Map.Entry.class)) {
      debug(1, node, "Derives Map.Entry: %s", type.getQualifiedName());
      return true;
    }
    debug(node, "Not refcounted: %s", type.getQualifiedName());
    return false;
  }

  protected final boolean isTempIdentifier(@NotNull SimpleName name) {
    return Pattern.matches(tempVarPrefix + "[\\d_]+", name.toString());
  }

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

  protected final boolean skip(@NotNull ASTNode node) {
    return enclosingMethods(node).stream().anyMatch(enclosingMethod -> {
      if (ASTUtil.hasAnnotation(enclosingMethod, RefIgnore.class)) return true;
      final String methodName = enclosingMethod.getName();
      return methodName.equals("addRefs") || methodName.equals("freeRefs");
    });
  }

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
