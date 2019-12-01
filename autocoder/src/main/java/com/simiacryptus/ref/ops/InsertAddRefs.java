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

import com.simiacryptus.ref.lang.RefCoderIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RefCoderIgnore
public class InsertAddRefs extends RefFileAstVisitor {

  public InsertAddRefs(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @NotNull
  public MethodInvocation insertAddRef(@NotNull Expression expression, @NotNull ITypeBinding type) {
    AST ast = expression.getAST();
    if (type.isArray()) {
      final String qualifiedName = type.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    }
    if (null == getAddRefMethod(type)) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    }
  }

  @Nullable
  public static IMethodBinding getAddRefMethod(@NotNull ITypeBinding type) {
    return Arrays.stream(type.getDeclaredMethods()).filter(method ->
        method.getName().equals("addRef") && (0 != (method.getModifiers() & Modifier.PUBLIC)) && method.getParameterTypes().length == 0
    ).findAny().orElse(null);
  }

  public void apply(ASTNode node, @NotNull List<ASTNode> arguments, String name) {
    for (int i = 0; i < arguments.size(); i++) {
      ASTNode arg = arguments.get(i);
      if (arg instanceof ClassInstanceCreation) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      } else if (arg instanceof AnonymousClassDeclaration) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      } else if (arg instanceof MethodInvocation) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      } else if (arg instanceof ArrayCreation) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), name);
      } else if (arg instanceof Expression) {
        final Expression expression = (Expression) arg;
        final ITypeBinding resolveTypeBinding = expression.resolveTypeBinding();
        if (null == resolveTypeBinding) {
          warn(arg, "Unresolved binding");
          return;
        }
        if (isRefCounted(arg, resolveTypeBinding)) {
          arguments.set(i, insertAddRef(expression, resolveTypeBinding));
          info(node, "Argument addRef for %s: %s (%s) defined", name, resolveTypeBinding.getQualifiedName(), expression);
        } else {
          info(node, "Non-refcounted arg %s in %s", expression, name);
        }
      } else {
        warn(node, "Unexpected type %s in %s", arg.getClass().getSimpleName(), name);
      }
    }
  }

  @Override
  public void endVisit(@NotNull ArrayInitializer node) {
    if (skip(node)) return;
    final ITypeBinding typeBinding = node.resolveTypeBinding();
    if (null != typeBinding) {
      if (modifyArgs(typeBinding.getElementType())) {
        final List expressions = node.expressions();
        for (int i = 0; i < expressions.size(); i++) {
          Object next = expressions.get(i);
          MethodInvocation methodInvocation = wrapAddRef((ASTNode) next);
          if (null != methodInvocation) {
            info(node, "Argument addRef for %s", next);
            expressions.set(i, methodInvocation);
          }
        }
      }
    }
  }

  @Override
  public void endVisit(@NotNull MethodInvocation node) {
    final Expression expression = node.getExpression();
    info(node, "Processing method %s.%s", null==expression?"?":expression.toString(), node.getName());
    if (skip(node)) return;
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding on %s", node);
      return;
    }
    final String targetLabel;
    if(null != expression && expression instanceof Name) {
      targetLabel = expression.toString() + "." + node.getName();
    } else {
      targetLabel = methodBinding.getDeclaringClass().getQualifiedName() + "::" + node.getName();
    }
    if (consumesRefs(methodBinding, null==expression?null:expression.resolveTypeBinding())) {
      apply(node, node.arguments(), targetLabel);
    } else {
      info(node, "Ignored method %s", targetLabel);
    }
  }

  @Override
  public void endVisit(@NotNull ConstructorInvocation node) {
    if (skip(node)) return;
    final IMethodBinding methodBinding = node.resolveConstructorBinding();
    if (null == methodBinding) {
      warn(node, "Cannot resolve " + node);
      return;
    }
    if (consumesRefs(methodBinding, methodBinding.getReturnType()) && node.arguments().size() > 0) {
      apply(node, node.arguments(), methodBinding.getReturnType().getQualifiedName());
    }
  }

  @Override
  public void endVisit(@NotNull ClassInstanceCreation node) {
    if (skip(node)) return;
    final IMethodBinding methodBinding = node.resolveConstructorBinding();
    if (null == methodBinding) {
      warn(node, "Cannot resolve %s", node);
      return;
    }
    if (consumesRefs(methodBinding, node.resolveTypeBinding())) {
      if (node.arguments().size() > 0) {
        apply(node, node.arguments(), methodBinding.getReturnType().getQualifiedName());
      } else {
        debug(node, "No args %s", node);
      }
    } else {
      info(node, "Non-refcounted constructor %s", node);
    }
  }

  public boolean modifyArgs(@NotNull ITypeBinding declaringClass) {
    return isRefAware(declaringClass);
  }

  @Nullable
  public MethodInvocation wrapAddRef(ASTNode node) {
    if (node instanceof SimpleName) {
      final SimpleName name = (SimpleName) node;
      if (derives(name.resolveTypeBinding(), ReferenceCounting.class)) {
        return (MethodInvocation) wrapAddRef(name, name.resolveTypeBinding());
      }
    }
    return null;
  }
}
