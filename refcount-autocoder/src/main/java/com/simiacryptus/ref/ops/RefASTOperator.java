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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * This class is responsible for creating a mapping of files to integers and
 * maintaining a count of identifiers for each file.
 *
 * @docgenVersion 9
 */
@RefIgnore
abstract class RefASTOperator extends ASTOperator {
  @Nonnull
  private static Map<File, Integer> fileCounters = new HashMap<>();
  @Nonnull
  private static Map<File, AtomicInteger> identifierCounters = new HashMap<>();
  @Nonnull
  private String tempVarPrefix = "temp";

  public RefASTOperator(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
    setFailAtEnd(true);
  }

  /**
   * Returns the addRef method for the given type, if it exists.
   *
   * @param type the type to check
   * @return the addRef method, or null if it does not exist
   * @docgenVersion 9
   */
  @Nullable
  public static IMethodBinding getAddRefMethod(@Nonnull ITypeBinding type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(method ->
        method.getName().equals("addRef")
            && 0 != (method.getModifiers() & Modifier.PUBLIC)
            && method.getParameterTypes().length == 0
    ).findAny().orElse(null);
  }

  /**
   * Returns the 'addRefs' method for the given type, or null if it does not exist.
   *
   * @param type the type to get the method for
   * @return the 'addRefs' method, or null
   * @docgenVersion 9
   */
  @Nullable
  public static IMethodBinding getAddRefsMethod(@Nonnull ITypeBinding type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(method ->
        method.getName().equals("addRef")
            && 0 != (method.getModifiers() & Modifier.PUBLIC)
            && 0 != (method.getModifiers() & Modifier.STATIC)
            && method.getParameterTypes().length == 1
    ).findAny().orElse(null);
  }

  /**
   * @param node
   * @return
   * @override
   * @docgenVersion 9
   */
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

  /**
   * Returns the {@link MethodDeclaration} node that corresponds to the given AST node,
   * or {@code null} if the given node does not represent a method declaration.
   *
   * @param node the AST node
   * @return the corresponding {@link MethodDeclaration} node, or {@code null} if the
   * given node does not represent a method declaration
   * @docgenVersion 9
   */
  @Nullable
  @SuppressWarnings("unused")
  public MethodDeclaration getMethodDeclaration(@Nullable ASTNode node) {
    if (null == node) return null;
    if (node instanceof MethodDeclaration) return (MethodDeclaration) node;
    if (node instanceof Statement) return null;
    if (node instanceof TypeDeclaration) return null;
    return getMethodDeclaration(node.getParent());
  }

  /**
   * Returns the statement node for the given AST node.
   *
   * @param node the AST node
   * @return the statement node
   * @docgenVersion 9
   */
  @Nullable
  @SuppressWarnings("unused")
  public Statement getStatement(@Nullable ASTNode node) {
    if (null == node) return null;
    if (node instanceof Statement) return (Statement) node;
    if (node instanceof MethodDeclaration) return null;
    if (node instanceof TypeDeclaration) return null;
    return getStatement(node.getParent());
  }

  /**
   * @param node    the node to check
   * @param binding the binding of the node
   * @return true if the node should be skipped, false otherwise
   * @docgenVersion 9
   */
  protected boolean skip(@Nonnull ASTNode node, IBinding binding) {
    if (ASTUtil.hasAnnotation(binding, RefIgnore.class)) {
      debug(node, "Marked with RefIgnore");
      return true;
    }
    return false;
  }

  /**
   * @param methodBinding
   * @param index
   * @return whether the given method binding consumes references at the given index
   * @docgenVersion 9
   */
  protected final boolean consumesRefs(@Nonnull IMethodBinding methodBinding, int index) {
    if (ASTUtil.hasAnnotation(methodBinding, RefIgnore.class)) return false;
    if (methodBinding.getName().equals("addRef")) {
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

  /**
   * @param node        the AST node
   * @param typeBinding the type binding
   * @return the statement
   * @throws IllegalArgumentException if the node is null
   * @docgenVersion 9
   */
  @Nonnull
  protected final Statement freeRefStatement(@Nonnull ASTNode node, @Nullable ITypeBinding typeBinding) {
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
   * @param node
   * @param typeBinding
   * @return
   * @docgenVersion 9
   */
  @Nonnull
  protected final Statement freeRefUtilStatement(@Nonnull ASTNode node, @Nonnull ITypeBinding typeBinding) {
    return ast.newExpressionStatement(newFreeRefUtil(node, typeBinding));
  }

  /**
   * Returns a temporary identifier for the given node.
   *
   * @param node the node to get a temporary identifier for
   * @return a temporary identifier for the given node
   * @docgenVersion 9
   */
  protected final String getTempIdentifier(@Nonnull ASTNode node) {
    final String id = String.format(tempVarPrefix + "_%02d_%04d",
        (long) fileCounters.computeIfAbsent(file, x -> fileCounters.size()),
        (long) identifierCounters.computeIfAbsent(file, x -> new AtomicInteger(0)).incrementAndGet());
    debug(1, node, "Creating %s", id);
    return id;
  }

  /**
   * Returns the type binding for the given variable declaration, or null if none.
   *
   * @param declaration the variable declaration
   * @return the type binding, or null
   * @docgenVersion 9
   */
  @Nullable
  protected final ITypeBinding getTypeBinding(@Nonnull VariableDeclaration declaration) {
    final IVariableBinding iVariableBinding = resolveBinding(declaration);
    if (iVariableBinding == null) {
      warn(1, declaration, "Cannot resolve method of %s", declaration);
      return null;
    }
    return iVariableBinding.getType();
  }

  /**
   * Returns true if the given node is a reference counted type.
   *
   * @param node the node to check
   * @param type the type of the node
   * @return true if the node is a reference counted type, false otherwise
   * @docgenVersion 9
   */
  protected final boolean isRefCounted(@Nonnull ASTNode node, @Nonnull ITypeBinding type) {
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

  /**
   * Returns true if the given name is a temporary identifier.
   *
   * @param name the name to check
   * @return true if the name is a temporary identifier
   * @docgenVersion 9
   */
  protected final boolean isTempIdentifier(@Nonnull SimpleName name) {
    return Pattern.matches(tempVarPrefix + "[\\d_]+", name.toString());
  }

  /**
   * Returns true if the given method consumes self-references.
   *
   * @param methodBinding the method binding to check
   * @return true if the given method consumes self-references, false otherwise
   * @docgenVersion 9
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
   * Creates a new addRef AST node.
   *
   * @param name        the name of the node
   * @param typeBinding the type binding of the node
   * @return the new AST node
   * @docgenVersion 9
   */
  @Nonnull
  protected final MethodInvocation newAddRef(@Nonnull ASTNode name, @Nonnull ITypeBinding typeBinding) {
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
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
   * Creates a new free reference node.
   *
   * @param node        the AST node
   * @param typeBinding the type binding
   * @return the new free reference node
   * @docgenVersion 9
   */
  @Nonnull
  protected final Statement newFreeRef(@Nonnull ASTNode node, @Nonnull ITypeBinding typeBinding) {
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
   * Creates a new {@link MethodInvocation} for a free ref util.
   *
   * @param name        the name of the util
   * @param typeBinding the type binding of the util
   * @return the new {@link MethodInvocation}
   * @docgenVersion 9
   */
  @Nonnull
  protected final MethodInvocation newFreeRefUtil(@Nonnull ASTNode name, @Nonnull ITypeBinding typeBinding) {
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
   * @param node the node to check
   * @return true if the node should be skipped, false otherwise
   * @docgenVersion 9
   */
  protected final boolean skip(@Nonnull ASTNode node) {
    return enclosingMethods(node).stream().anyMatch(enclosingMethod -> {
      if (ASTUtil.hasAnnotation(enclosingMethod, RefIgnore.class)) return true;
      final String methodName = enclosingMethod.getName();
      return methodName.equals("addRef") || methodName.equals("freeRefs");
    });
  }

  /**
   * @param expression the expression to wrap
   * @param type       the type of the expression
   * @return the wrapped expression
   * @docgenVersion 9
   */
  @Nonnull
  protected final Expression wrapAddRef(@Nonnull Expression expression, @Nonnull ITypeBinding type) {
    if (expression instanceof CastExpression) {
      final ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
      replace(expression, parenthesizedExpression);
      parenthesizedExpression.setExpression(expression);
      return wrapAddRef(parenthesizedExpression, type);
    }
    if (type.isArray()) {
      if (null == getAddRefsMethod(type.getElementType())) {
        warn(1, expression, "No AddRefs method defined for %s", expression);
        return copyIfAttached(expression);
      } else {
        final String qualifiedName = type.getElementType().getQualifiedName();
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRef"));
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
