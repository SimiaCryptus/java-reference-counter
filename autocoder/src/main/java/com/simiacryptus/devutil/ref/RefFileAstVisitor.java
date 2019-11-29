package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.ops.FileAstVisitor;
import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Optional;

abstract class RefFileAstVisitor extends FileAstVisitor {
  public RefFileAstVisitor(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @NotNull
  protected IfStatement freeRefStatement(@NotNull ASTNode node, ITypeBinding typeBinding) {
    AST ast = node.getAST();
    final IfStatement ifStatement = ast.newIfStatement();
    final InfixExpression infixExpression = ast.newInfixExpression();
    infixExpression.setLeftOperand(ast.newNullLiteral());
    infixExpression.setRightOperand((Expression) ASTNode.copySubtree(ast, node));
    infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
    ifStatement.setExpression(infixExpression);
    if (null == typeBinding) {
      info(node, "Cannot add freeRef (binding not resolved)");
    } else {
      ifStatement.setThenStatement(ast.newExpressionStatement(newFreeRef(node, typeBinding)));
      info(1, node, "Added freeRef");
    }
    return ifStatement;
  }

  @NotNull
  protected Statement freeRefUtilStatement(@NotNull ASTNode node, ITypeBinding typeBinding) {
    final AST ast = node.getAST();
    return ast.newExpressionStatement(newFreeRefUtil(node, typeBinding));
  }

  protected boolean isRefCounted(ASTNode node, @NotNull ITypeBinding typeBinding) {
    final ITypeBinding type;
    if (typeBinding.isPrimitive()) return false;
    if (typeBinding.getTypeDeclaration().getQualifiedName().equals(Optional.class.getCanonicalName())) {
      final ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
      if(null == typeArguments || 0 == typeArguments.length) {
        warn(node,"No type argument for Optional");
      }
      return isRefCounted(node, typeArguments[0]);
    }
    if (typeBinding.isArray()) {
      type = typeBinding.getElementType();
    } else {
      type = typeBinding;
    }
    return derives(type, ReferenceCounting.class);
  }

  protected boolean methodConsumesSelfRefs(@Nonnull IMethodBinding methodBinding) {
    final String qualifiedName = methodBinding.getDeclaringClass().getTypeDeclaration().getQualifiedName();
    final String methodName = methodBinding.getName();
    if (qualifiedName.equals(Optional.class.getCanonicalName())) {
      if (methodName.equals("get")) {
        return true;
      }
    }
    return false;
  }

  protected boolean methodConsumesRefs(@Nonnull IMethodBinding methodBinding, ASTNode node) {
    final String qualifiedName = methodBinding.getDeclaringClass().getQualifiedName();
    final String methodName = methodBinding.getName();
    if (qualifiedName.equals(String.class.getCanonicalName())) {
      if (methodName.equals("format")) {
        return false;
      }
    }
    //Arrays.toString
    if (methodName.equals("addRefs")) {
      return false;
    }
    if (methodName.equals("freeRefs")) {
      return false;
    }
    if (toString(methodBinding.getDeclaringClass().getPackage()).startsWith("com.simiacryptus")) return true;
    warn(1, node, "Not sure if %s consumes refs", methodBinding);
    return true;
  }

  @NotNull
  protected MethodInvocation newAddRef(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, name));
      return methodInvocation;
    }
  }

  @NotNull
  protected MethodInvocation newFreeRef(@NotNull ASTNode node, @NotNull ITypeBinding typeBinding) {
    AST ast = node.getAST();
    if (typeBinding.isArray()) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, ReferenceCounting.class));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, node));
      return methodInvocation;
    } else if (typeBinding.getTypeDeclaration().getQualifiedName().equals(Optional.class.getCanonicalName())) {
      final ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
      if(null == typeArguments || 0 == typeArguments.length) {
        warn(node,"No type argument for Optional");
      }
      final MethodInvocation freeInvocation = ast.newMethodInvocation();
      freeInvocation.setName(ast.newSimpleName("freeRef"));
      freeInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
      freeInvocation.arguments().add((Expression) ASTNode.copySubtree(ast, node));
      return freeInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, node));
      return methodInvocation;
    }
  }

  @NotNull
  protected MethodInvocation newFreeRefUtil(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, ReferenceCounting.class));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, name));
      return methodInvocation;
    }
  }

  protected boolean skip(@NotNull ASTNode node) {
    return enclosingMethods(node).stream().filter(enclosingMethod -> {
      final String methodName = enclosingMethod.getName();
      return methodName.equals("addRefs") || methodName.equals("freeRefs");
    }).findFirst().isPresent();
  }

  @NotNull
  protected Expression wrapAddRef(@NotNull Expression expression, @Nullable ITypeBinding typeBinding) {
    AST ast = expression.getAST();
    if (null == typeBinding) {
      return expression;
    } else if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    }
  }
}
