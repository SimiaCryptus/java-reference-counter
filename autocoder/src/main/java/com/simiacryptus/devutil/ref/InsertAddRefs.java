package com.simiacryptus.devutil.ref;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

class InsertAddRefs extends RefFileAstVisitor {

  InsertAddRefs(RefAutoCoder refAutoCoder, CompilationUnit compilationUnit, File file) {
    super(refAutoCoder, compilationUnit, file);
  }

  @NotNull
  public MethodInvocation addAddRef(@NotNull Expression expression, @NotNull ITypeBinding type) {
    AST ast = expression.getAST();
    if (type.isArray()) {
      final String qualifiedName = type.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    } if (type.isInterface()) {
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

  public void apply(ASTNode methodNode, @NotNull ASTNode node, @NotNull List<ASTNode> arguments) {
    for (int i = 0; i < arguments.size(); i++) {
      ASTNode arg = arguments.get(i);
      if (arg instanceof ClassInstanceCreation) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
      } else if (arg instanceof AnonymousClassDeclaration) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
      } else if (arg instanceof MethodInvocation) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
      } else if (arg instanceof ArrayCreation) {
        debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
      } else if (arg instanceof Expression) {
        final Expression expression = (Expression) arg;
        final ITypeBinding resolveTypeBinding = expression.resolveTypeBinding();
        if(null == resolveTypeBinding) {
          warn(arg, "Unresolved binding");
          return;
        }
        if (isRefCounted(arg, resolveTypeBinding)) {
          arguments.set(i, addAddRef(expression, resolveTypeBinding));
          info(node, "Argument addRef for %s: %s (%s) defined", node, resolveTypeBinding.getQualifiedName(), expression);
        } else {
          info(node, "Non-refcounted arg %s", expression);
        }
      } else {
        warn(node, "Unexpected type %s", arg.getClass().getSimpleName());
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
    if (skip(node)) return;
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding on %s", node);
      return;
    }
//        if (modifyArgs(methodBinding.getDeclaringClass()) && !node.getName().toString().equals("addRefs")) {
//          final List arguments = node.arguments();
//          for (int i = 0; i < arguments.size(); i++) {
//            Object next = arguments.get(i);
//            MethodInvocation methodInvocation = wrapAddRef((ASTNode) next);
//            if (null != methodInvocation) {
//              final SimpleName nodeName = node.getName();
//              info(node, "Argument addRef for %s: %s", nodeName, nodeName.resolveTypeBinding().getQualifiedName());
//              arguments.set(i, methodInvocation);
//            }
//          }
//        }
    if (methodConsumesRefs(methodBinding, node)) {
      apply(node, node.getName(), node.arguments());
    } else {
      debug(node, "Ignored method on %s", node);
    }
  }

  @Override
  public void endVisit(@NotNull ConstructorInvocation node) {
    if (skip(node)) return;
    final IMethodBinding methodBinding = node.resolveConstructorBinding();
    if (null != methodBinding) {
      if (methodConsumesRefs(methodBinding, node) && node.arguments().size() > 0) {
        apply(node, node, node.arguments());
      }
    } else {
      warn(node, "Cannot resolve " + node);
    }
  }

  @Override
  public void endVisit(@NotNull ClassInstanceCreation node) {
    if (skip(node)) return;
    final IMethodBinding methodBinding = node.resolveConstructorBinding();
    if (null != methodBinding) {
      if (methodConsumesRefs(methodBinding, node)) {
        if (node.arguments().size() > 0) {
          apply(node, node.getType(), node.arguments());
        } else {
          debug(node, "No args %s", node);
        }
      } else {
        info(node, "Non-refcounted arg %s", node);
      }
    } else {
      warn(node, "Cannot resolve %s", node);
    }
  }

  public boolean modifyArgs(@NotNull ITypeBinding declaringClass) {
    return refAutoCoder.isRefAware(declaringClass);
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
