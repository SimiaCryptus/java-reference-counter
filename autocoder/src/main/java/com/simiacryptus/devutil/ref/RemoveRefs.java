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

package com.simiacryptus.devutil.ref;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

class RemoveRefs extends RefFileAstVisitor {

  RemoveRefs(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    if (derives(typeBinding, ReferenceCounting.class)) {
      removeMethods(node, "addRef");
      removeMethods(node, "freeRef");
      //removeMethods(node, "_free");
      removeMethods(node, "addRefs");
      removeMethods(node, "freeRefs");
    }
  }

  @Override
  public void endVisit(@NotNull MethodInvocation node) {
    final String methodName = node.getName().toString();
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved method binding %s", node);
      return;
    }
    final String declaringClass = methodBinding.getDeclaringClass().getQualifiedName();
    if (Arrays.asList("addRef", "freeRef", "addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
      final AST ast = node.getAST();
      final Expression subject;
      if (Arrays.asList("addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
        subject = (Expression) ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(0));
      } else if (declaringClass.equals(RefUtil.class.getCanonicalName())) {
        subject = (Expression) ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(0));
      } else {
        subject = (Expression) ASTNode.copySubtree(ast, node.getExpression());
      }
      info(node, "Removing %s and replacing with %s", methodName, subject);
      //        replace(node, subject);
      final ASTNode parent = node.getParent();
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
        if (subject == null) {
          info(node, "%s removed", parent);
          delete((ExpressionStatement) parent);
        } else if (subject instanceof Name) {
          info(node, "%s removed", parent);
          delete((ExpressionStatement) parent);
        } else if (subject instanceof FieldAccess) {
          info(node, "%s removed", parent);
          delete((ExpressionStatement) parent);
        } else {
          info(node, "%s replaced with %s", parent, subject);
          replace(parent, ast.newExpressionStatement(subject));
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
        warn(node, "Cannot remove %s called in %s: %s", node, parent.getClass(), parent);
      }
    }
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    if (derives(typeBinding, ReferenceCounting.class)) {
      removeMethods(node, "_free");
    }
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
