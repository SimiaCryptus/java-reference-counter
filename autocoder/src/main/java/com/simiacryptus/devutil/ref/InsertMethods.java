package com.simiacryptus.devutil.ref;

import com.simiacryptus.lang.ref.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

class InsertMethods extends RefFileAstVisitor {

  public InsertMethods(CompilationUnit cu, File file) {
    super(cu, file);
  }

  @Override
  public void endVisit(@NotNull TypeDeclaration node) {
    if (derives(node.resolveBinding(), ReferenceCounting.class)) {
      final AST ast = node.getAST();
      final List declarations = node.bodyDeclarations();
      declarations.add(method_free(ast));
      declarations.add(method_addRef(ast, node.getName()));
      declarations.add(method_addRefs(ast, node.getName()));
      //declarations.add(method_freeRefs(ast, node.getName()));
    }
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {
    if (derives(node.resolveBinding(), ReferenceCounting.class)) {
      final AST ast = node.getAST();
      final List declarations = node.bodyDeclarations();
      declarations.add(method_free(ast));
    }
  }

  @NotNull
  public MethodDeclaration method_addRef(@NotNull AST ast, @NotNull SimpleName name) {
    final String fqTypeName = name.getFullyQualifiedName();
    final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
    methodDeclaration.setName(ast.newSimpleName("addRef"));
    methodDeclaration.setReturnType2(ast.newSimpleType(ast.newSimpleName(fqTypeName)));
    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
    methodDeclaration.modifiers().add(annotation_override(ast));
    final Block block = ast.newBlock();
    final CastExpression castExpression = ast.newCastExpression();
    castExpression.setType(ast.newSimpleType(ast.newSimpleName(fqTypeName)));
    final SuperMethodInvocation superMethodInvocation = ast.newSuperMethodInvocation();
    superMethodInvocation.setName(ast.newSimpleName("addRef"));
    castExpression.setExpression(superMethodInvocation);
    final ReturnStatement returnStatement = ast.newReturnStatement();
    returnStatement.setExpression(castExpression);
    block.statements().add(returnStatement);
    methodDeclaration.setBody(block);
    return methodDeclaration;
  }

  @NotNull
  public MethodDeclaration method_addRefs(@NotNull AST ast, @NotNull SimpleName name) {
    final String fqTypeName = name.getFullyQualifiedName();
    final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
    methodDeclaration.setName(ast.newSimpleName("addRefs"));

    methodDeclaration.setReturnType2(arrayType(ast, fqTypeName));
    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));

    final SingleVariableDeclaration arg = ast.newSingleVariableDeclaration();
    arg.setType(arrayType(ast, fqTypeName));
    arg.setName(ast.newSimpleName("array"));
    methodDeclaration.parameters().add(arg);

    final MethodInvocation stream_invoke = ast.newMethodInvocation();
    stream_invoke.setExpression(newQualifiedName(ast, "java.util.Arrays".split("\\.")));
    stream_invoke.setName(ast.newSimpleName("stream"));
    stream_invoke.arguments().add(ast.newSimpleName("array"));

    final MethodInvocation filter_invoke = ast.newMethodInvocation();
    {
      filter_invoke.setExpression(stream_invoke);
      filter_invoke.setName(ast.newSimpleName("filter"));
      final LambdaExpression filter_lambda = ast.newLambdaExpression();
      final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
      variableDeclarationFragment.setName(ast.newSimpleName("x"));
      filter_lambda.parameters().add(variableDeclarationFragment);
      final InfixExpression infixExpression = ast.newInfixExpression();
      infixExpression.setLeftOperand(ast.newSimpleName("x"));
      infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
      infixExpression.setRightOperand(ast.newNullLiteral());
      filter_lambda.setBody(infixExpression);
      filter_invoke.arguments().add(filter_lambda);
    }

    final MethodInvocation addref_invoke = ast.newMethodInvocation();
    {
      addref_invoke.setExpression(filter_invoke);
      addref_invoke.setName(ast.newSimpleName("map"));
      final ExpressionMethodReference body = ast.newExpressionMethodReference();
      body.setExpression(ast.newSimpleName(fqTypeName));
      body.setName(ast.newSimpleName("addRef"));
      addref_invoke.arguments().add(body);
    }

    final MethodInvocation toArray_invoke = ast.newMethodInvocation();
    {
      toArray_invoke.setExpression(addref_invoke);
      toArray_invoke.setName(ast.newSimpleName("toArray"));
      final LambdaExpression filter_lambda = ast.newLambdaExpression();
      final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
      variableDeclarationFragment.setName(ast.newSimpleName("x"));
      filter_lambda.parameters().add(variableDeclarationFragment);

      final ArrayCreation arrayCreation = ast.newArrayCreation();
      arrayCreation.setType(arrayType(ast, fqTypeName));
      arrayCreation.dimensions().add(ast.newSimpleName("x"));

      filter_lambda.setBody(arrayCreation);
      toArray_invoke.arguments().add(filter_lambda);
    }

    final Block block = ast.newBlock();
    final ReturnStatement returnStatement = ast.newReturnStatement();
    returnStatement.setExpression(toArray_invoke);
    block.statements().add(returnStatement);
    methodDeclaration.setBody(block);
    return methodDeclaration;
  }

  @NotNull
  public MethodDeclaration method_free(@NotNull AST ast) {
    final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
    methodDeclaration.setName(ast.newSimpleName("_free"));
    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
    methodDeclaration.modifiers().add(annotation_override(ast));
    final Block body = ast.newBlock();
    final SuperMethodInvocation superCall = ast.newSuperMethodInvocation();
    superCall.setName(ast.newSimpleName("_free"));
    body.statements().add(ast.newExpressionStatement(superCall));
    methodDeclaration.setBody(body);
    return methodDeclaration;
  }

  @NotNull
  public MethodDeclaration method_freeRefs(@NotNull AST ast, @NotNull SimpleName name) {
    final String fqTypeName = name.getFullyQualifiedName();
    final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
    methodDeclaration.setName(ast.newSimpleName("freeRefs"));

    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));

    final SingleVariableDeclaration arg = ast.newSingleVariableDeclaration();
    arg.setType(arrayType(ast, fqTypeName));
    arg.setName(ast.newSimpleName("array"));
    methodDeclaration.parameters().add(arg);

    final MethodInvocation stream_invoke = ast.newMethodInvocation();
    stream_invoke.setExpression(newQualifiedName(ast, "java.util.Arrays".split("\\.")));
    stream_invoke.setName(ast.newSimpleName("stream"));
    stream_invoke.arguments().add(ast.newSimpleName("array"));

    final MethodInvocation filter_invoke = ast.newMethodInvocation();
    {
      filter_invoke.setExpression(stream_invoke);
      filter_invoke.setName(ast.newSimpleName("filter"));
      final LambdaExpression filter_lambda = ast.newLambdaExpression();
      final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
      variableDeclarationFragment.setName(ast.newSimpleName("x"));
      filter_lambda.parameters().add(variableDeclarationFragment);
      final InfixExpression infixExpression = ast.newInfixExpression();
      infixExpression.setLeftOperand(ast.newSimpleName("x"));
      infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
      infixExpression.setRightOperand(ast.newNullLiteral());
      filter_lambda.setBody(infixExpression);
      filter_invoke.arguments().add(filter_lambda);
    }

    final MethodInvocation addref_invoke = ast.newMethodInvocation();
    {
      addref_invoke.setExpression(filter_invoke);
      addref_invoke.setName(ast.newSimpleName("forEach"));
      final ExpressionMethodReference body = ast.newExpressionMethodReference();
      body.setExpression(ast.newSimpleName(fqTypeName));
      body.setName(ast.newSimpleName("freeRef"));
      addref_invoke.arguments().add(body);
    }

    final Block block = ast.newBlock();
    block.statements().add(ast.newExpressionStatement(addref_invoke));
    methodDeclaration.setBody(block);
    return methodDeclaration;
  }

}
