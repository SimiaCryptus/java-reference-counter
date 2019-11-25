package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.AutoCoder;
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
    if (AutoCoder.derives(typeBinding, ReferenceCounting.class)) {
      removeMethods(node, "addRef");
      removeMethods(node, "freeRef");
      removeMethods(node, "_free");
      removeMethods(node, "addRefs");
      removeMethods(node, "freeRefs");
    }
  }

  @Override
  public void endVisit(@NotNull MethodInvocation node) {
    final String methodName = node.getName().toString();
    if (Arrays.asList("addRef", "freeRef", "addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
      final AST ast = node.getAST();
      final Expression subject;
      if (Arrays.asList("addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
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
        if (subject instanceof Name) {
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
    if (AutoCoder.derives(typeBinding, ReferenceCounting.class)) {
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
