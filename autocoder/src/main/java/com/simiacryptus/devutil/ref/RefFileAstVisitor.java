package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.AutoCoder;
import com.simiacryptus.devutil.ops.FileAstVisitor;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;

abstract class RefFileAstVisitor extends FileAstVisitor {
  public RefFileAstVisitor(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @NotNull
  public IfStatement freeRefStatement(@NotNull ASTNode node, ITypeBinding typeBinding) {
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
      info(node, "Added freeRef");
    }
    return ifStatement;
  }

  public boolean isRefCounted(@NotNull ITypeBinding resolveTypeBinding) {
    final ITypeBinding type;
    if (resolveTypeBinding.isArray()) {
      type = resolveTypeBinding.getElementType();
    } else {
      type = resolveTypeBinding;
    }
    return AutoCoder.derives(type, ReferenceCounting.class);
  }

  public boolean methodConsumesRefs(@Nonnull IMethodBinding methodBinding, ASTNode node) {
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
    if (AutoCoder.toString(methodBinding.getDeclaringClass().getPackage()).startsWith("com.simiacryptus")) return true;
    warn(node, "Not sure if %s consumes refs", methodBinding);
    return true;
  }

  @NotNull
  public MethodInvocation newAddRef(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(AutoCoder.newQualifiedName(ast, qualifiedName.split("\\.")));
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
  public MethodInvocation newFreeRef(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRefs"));
      methodInvocation.setExpression(AutoCoder.newQualifiedName(ast, ReferenceCounting.class));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, name));
      return methodInvocation;
    }
  }

  @NotNull
  public Expression wrapAddRef(@NotNull Expression expression, @Nullable ITypeBinding typeBinding) {
    AST ast = expression.getAST();
    if (null == typeBinding) {
      return expression;
    } else if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(AutoCoder.newQualifiedName(ast, qualifiedName.split("\\.")));
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
