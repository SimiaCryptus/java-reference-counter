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

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.ops.FileAstVisitor;
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
import java.util.Map;
import java.util.Optional;

@RefIgnore
abstract class RefFileAstVisitor extends FileAstVisitor {

  public RefFileAstVisitor(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Nullable
  public static IMethodBinding getAddRefMethod(@NotNull ITypeBinding type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(method ->
        method.getName().equals("addRef") && (0 != (method.getModifiers() & Modifier.PUBLIC)) && method.getParameterTypes().length == 0
    ).findAny().orElse(null);
  }

  protected final boolean consumesRefs(@Nonnull IMethodBinding methodBinding, ITypeBinding expression) {
    final String methodName = methodBinding.getName();
    if (hasAnnotation(methodBinding, RefIgnore.class)) return false;
    if (methodName.equals("addRefs")) {
      return false;
    }
    if (methodName.equals("freeRefs")) {
      return false;
    }
    return null == expression ? isRefAware(methodBinding.getDeclaringClass()) : isRefAware(expression);
  }

  @NotNull
  protected final IfStatement freeRefStatement(@NotNull ASTNode node, ITypeBinding typeBinding) {
    AST ast = node.getAST();
    final IfStatement ifStatement = ast.newIfStatement();
    final InfixExpression infixExpression = ast.newInfixExpression();
    infixExpression.setLeftOperand(ast.newNullLiteral());
    infixExpression.setRightOperand((Expression) copySubtree(ast, node));
    infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
    ifStatement.setExpression(infixExpression);
    if (null == typeBinding) {
      info(1, node, "Cannot add freeRef (binding not resolved)");
    } else {
      ifStatement.setThenStatement(ast.newExpressionStatement(newFreeRef(node, typeBinding)));
      info(1, node, "Added freeRef");
    }
    return ifStatement;
  }

  @NotNull
  protected final Statement freeRefUtilStatement(@NotNull ASTNode node, ITypeBinding typeBinding) {
    final AST ast = node.getAST();
    return ast.newExpressionStatement(newFreeRefUtil(node, typeBinding));
  }

  protected final ITypeBinding getTypeBinding(@NotNull VariableDeclaration declaration) {
    final IVariableBinding iVariableBinding = resolveBinding(declaration);
    if (iVariableBinding == null) {
      warn(1, declaration, "Cannot resolve method of %s", declaration);
      return null;
    }
    return iVariableBinding.getType();
  }

  protected final boolean isRefAware(ITypeBinding declaringClass) {
    if (declaringClass.getTypeDeclaration().getQualifiedName().equals(Map.Entry.class.getCanonicalName())) return true;
    return hasAnnotation(declaringClass, RefAware.class);
  }

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
    if (type.isInterface() && !(node instanceof LambdaExpression)) {
      info(1, node, "Is potentially refcounted interface: %s (%s)", node, type.getQualifiedName());
      return true;
    }
    if (derives(type, ReferenceCounting.class)) {
      info(1, node, "Derives ReferenceCounting: %s (%s)", node, type.getQualifiedName());
      return true;
    }
    if (derives(type, Map.Entry.class)) {
      info(1, node, "Derives Map.Entry: %s (%s)", node, type.getQualifiedName());
      return true;
    }
//    if (isRefAware(typeBinding)) {
//      info(1, node, "Marked with @RefAware: %s (%s)", node, type);
//      return true;
//    }
    debug(node, "Not refcounted: %s (%s)", node, type.getQualifiedName());
    return false;
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
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(copySubtree(ast, name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression((Expression) copySubtree(ast, name));
      return methodInvocation;
    }
  }

  @NotNull
  protected final MethodInvocation newFreeRef(@NotNull ASTNode node, @NotNull ITypeBinding typeBinding) {
    AST ast = node.getAST();
    if (typeBinding.isArray()) {
      if (derives(typeBinding.getElementType(), ReferenceCounting.class)) {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("freeRefs"));
        methodInvocation.setExpression(newQualifiedName(ast, ReferenceCounting.class));
        methodInvocation.arguments().add(copySubtree(ast, node));
        return methodInvocation;
      } else {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("freeRefs"));
        methodInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
        methodInvocation.arguments().add(copySubtree(ast, node));
        return methodInvocation;
      }
    } else if (derives(typeBinding, ReferenceCounting.class)) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression((Expression) copySubtree(ast, node));
      return methodInvocation;
    } else {
      final MethodInvocation freeInvocation = ast.newMethodInvocation();
      freeInvocation.setName(ast.newSimpleName("freeRef"));
      freeInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
      freeInvocation.arguments().add((Expression) copySubtree(ast, node));
      return freeInvocation;
    }
  }

  @NotNull
  protected final MethodInvocation newFreeRefUtil(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, ReferenceCounting.class));
      methodInvocation.arguments().add(copySubtree(ast, name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
      methodInvocation.arguments().add(copySubtree(ast, name));
      return methodInvocation;
    }
  }

  @Override
  public boolean preVisit2(ASTNode node) {
    if (node instanceof TypeDeclaration) {
      final ITypeBinding binding = FileAstVisitor.resolveBinding((TypeDeclaration) node);
      if (null == binding) {
        warn(node, "Unresolved binding");
        return false;
      }
      if (hasAnnotation(binding, RefIgnore.class)) {
        warn(node, "Marked with RefIgnore");
        return false;
      }
    }
    return super.preVisit2(node);
  }

  protected final boolean skip(@NotNull ASTNode node) {
    return enclosingMethods(node).stream().anyMatch(enclosingMethod -> {
      if (hasAnnotation(enclosingMethod, RefIgnore.class)) return true;
      final String methodName = enclosingMethod.getName();
      return methodName.equals("addRefs") || methodName.equals("freeRefs");
    });
  }

  @NotNull
  protected final Expression wrapAddRef(@NotNull Expression expression, @NotNull ITypeBinding type) {
    AST ast = expression.getAST();
    if (null == type) {
      warn(1, expression, "No type for %s; cannot addRef", expression);
      return (Expression) copySubtree(ast, expression);
    } else if (type.isArray()) {
      final String qualifiedName = type.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(copySubtree(ast, expression));
      info(1, expression, "AddRef for %s", expression);
      return methodInvocation;
    }
    if (null == getAddRefMethod(type)) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
      methodInvocation.arguments().add(copySubtree(ast, expression));
      info(1, expression, "AddRef for %s", expression);
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression((Expression) copySubtree(ast, expression));
      info(1, expression, "AddRef for %s", expression);
      return methodInvocation;
    }
  }

}
