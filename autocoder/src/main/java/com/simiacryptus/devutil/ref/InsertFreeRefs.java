package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.AutoCoder;
import com.simiacryptus.devutil.ops.IndexSymbols;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class InsertFreeRefs extends RefFileAstVisitor {

  InsertFreeRefs(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  public void addFreeRef(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding) {
    if (skip(declaration)) return;
    if (AutoCoder.derives(typeBinding, ReferenceCounting.class)) {
      final SimpleName name = declaration.getName();
      ASTNode parent = declaration.getParent();
      if (parent instanceof MethodDeclaration) {
        final MethodDeclaration node = (MethodDeclaration) parent;
        addFreeRef(typeBinding, name, node.getBody());
      } else if (parent instanceof LambdaExpression) {
        final LambdaExpression node = (LambdaExpression) parent;
        final ASTNode lambdaParent = node.getParent();
        final ASTNode body = node.getBody();
        if (body instanceof Block) {
          addFreeRef(typeBinding, name, (Block) body);
        } else {
          final AST ast = node.getAST();
          final Block block = ast.newBlock();
          if (hasReturnValue(lambdaParent, node)) {
            final ReturnStatement returnStatement = ast.newReturnStatement();
            returnStatement.setExpression((Expression) ASTNode.copySubtree(ast, body));
            block.statements().add(returnStatement);
          } else {
            block.statements().add(ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, body)));
          }
          node.setBody(block);
          addFreeRef(typeBinding, name, block);
        }
      } else if (parent instanceof VariableDeclarationStatement) {
        parent = parent.getParent();
        if (parent instanceof Block) {
          addFreeRef(typeBinding, name, (Block) parent);
        } else {
          warn(declaration, "Cannot add freeRef for %s (VariableDeclarationStatement) in %s : %s", name, parent.getClass(), parent.toString().trim());
        }
      } else if (parent instanceof FieldDeclaration) {
        add_freeRef_entry(declaration, typeBinding, name, parent);
      } else {
        warn(declaration, "Cannot add freeRef for %s in %s : %s",
            name,
            parent.getClass(), parent.toString().trim());
      }
    }
  }

  public void addFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName name, @NotNull Block body) {
    final List<IndexSymbols.Mention> lastMentions = AutoCoder.lastMentions(body, name.resolveBinding());
    lastMentions.stream().filter(x -> !x.isReturn())
        .forEach(insertFreeRef(typeBinding, name, body));
    lastMentions.stream().filter(x -> x.isComplexReturn())
        .forEach(insertAddRef_ComplexReturn(name, typeBinding));
  }

  public void add_freeRef_entry(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding, SimpleName name, ASTNode parent) {
    final ASTNode fieldParent = parent.getParent();
    if (fieldParent instanceof TypeDeclaration) {
      final TypeDeclaration typeDeclaration = (TypeDeclaration) fieldParent;
      final Optional<MethodDeclaration> freeMethodOpt = AutoCoder.findMethod(typeDeclaration, "_free");
      if (freeMethodOpt.isPresent()) {
        final AST ast = name.getAST();
        final MethodInvocation expression = newFreeRef(name, typeBinding);
        final ExpressionStatement expressionStatement = ast.newExpressionStatement(expression);
        final IfStatement ifStatement = ast.newIfStatement();
        final InfixExpression nullTest = ast.newInfixExpression();
        nullTest.setLeftOperand(ast.newNullLiteral());
        nullTest.setOperator(InfixExpression.Operator.NOT_EQUALS);
        nullTest.setRightOperand((Expression) ASTNode.copySubtree(ast, name));
        ifStatement.setExpression(nullTest);
        ifStatement.setThenStatement(expressionStatement);
        info(declaration, "Adding freeRef for %s::%s to %s", typeDeclaration.getName(), declaration.getName(), "(" + getLocation(name) + ")");
        freeMethodOpt.get().getBody().statements().add(0, ifStatement);
      } else {
        warn(declaration, "Cannot add freeRef for %s::%s - no _free method", typeDeclaration.getName(), declaration.getName());
      }
    } else {
      warn(declaration, "Cannot add freeRef for %s (FieldDeclaration) in %s : %s", name, fieldParent.getClass(), fieldParent.toString().trim());
    }
  }

  private void apply(@NotNull Expression node, ITypeBinding typeBinding) {
    if (isRefCounted(typeBinding)) {
      info(node, "Ref-returning method: %s", node);
      final ASTNode parent = node.getParent();
      if (parent instanceof MethodInvocation) {
        final MethodInvocation methodInvocation = (MethodInvocation) parent;
        final IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
        if (null == methodBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
        }
        if (AutoCoder.contains(methodInvocation.getExpression(), node) || !methodConsumesRefs(methodBinding, node)) {
          freeRefs(node, typeBinding);
        } else {
          info(node, "Result consumed by method");
        }
      } else if (parent instanceof FieldAccess) {
        final FieldAccess methodInvocation = (FieldAccess) parent;
        final IVariableBinding fieldBinding = methodInvocation.resolveFieldBinding();
        if (null == fieldBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
        }
        if (AutoCoder.contains(methodInvocation.getExpression(), node)) {
          freeRefs(node, typeBinding);
        } else {
          info(node, "Result consumed by method");
        }
      } else if (parent instanceof Statement) {
        final ASTNode statementParent = parent.getParent();
        if (statementParent instanceof Block) {
          final Block block = (Block) statementParent;
          final int lineNumber = block.statements().indexOf(parent);
          block.statements().set(lineNumber, freeRefStatement(node, typeBinding));
          info(node, "Wrapped method call with freeref at line " + lineNumber);
        } else {
          warn(node, "Non-block method call statement: %s (%s)", parent.getClass(), parent);
        }
      } else if (parent instanceof VariableDeclarationFragment) {
        info(node, "Local variable assigned to method: %s (%s)", parent.getClass(), parent);
      } else {
        warn(node, "Non-ExpressionStatement method: %s (%s)", parent.getClass(), parent);
      }
    } else {
      warn(node, "Ignored method returning %s: %s", typeBinding.getQualifiedName(), node);
    }
  }

  private void freeRefs(@NotNull Expression node, ITypeBinding typeBinding) {
    Statement statement = getStatement(node);
    final ASTNode statementParent = statement.getParent();
    AST ast = node.getAST();
    if (!(statementParent instanceof Block)) {
      //warn(node, "Non-block statement %s", statementParent.getClass().getSimpleName());
      Block block = ast.newBlock();
      final List statements = block.statements();
      final String identifier = randomIdentifier(node);
      final ExpressionStatement localVariable = AutoCoder.newLocalVariable(identifier, node, AutoCoder.getType(ast, typeBinding.getQualifiedName()));
      final ExpressionStatement freeStatement = ast.newExpressionStatement(newFreeRef(ast.newSimpleName(identifier), typeBinding));
      replace(node, ast.newSimpleName(identifier));
      statements.add(localVariable);
      statements.add(ASTNode.copySubtree(ast, statement));
      statements.add(freeStatement);
      replace(node, ast.newSimpleName(identifier));
      replace(statement, block);
      info(node, "Wrapped method call with freeref and new block");
    } else {
      Block block = (Block) statementParent;
      final List statements = block.statements();
      final int lineNumber = statements.indexOf(statement);
      final String identifier = randomIdentifier(node);
      final ExpressionStatement localVariable = AutoCoder.newLocalVariable(identifier, node, AutoCoder.getType(ast, typeBinding.getQualifiedName()));
      final ExpressionStatement freeStatement = ast.newExpressionStatement(newFreeRef(ast.newSimpleName(identifier), typeBinding));
      replace(node, ast.newSimpleName(identifier));
      if (lineNumber + 1 < statements.size()) {
        statements.add(lineNumber + 1, freeStatement);
      } else {
        statements.add(lineNumber + 1, freeStatement);
      }
      statements.add(lineNumber, localVariable);
      info(node, "Wrapped method call with freeref at line %s", lineNumber);
    }
  }

  @Override
  public void endVisit(@NotNull VariableDeclarationFragment declaration) {
    if (skip(declaration)) return;
    final ASTNode parent = declaration.getParent();
    if (parent instanceof VariableDeclarationStatement) {
      final ITypeBinding typeBinding = ((VariableDeclarationStatement) parent).getType().resolveBinding();
      if (null != typeBinding) {
        addFreeRef(declaration, typeBinding);
      } else {
        warn(declaration, "Cannot resolve type of %s", parent);
      }
    } else if (parent instanceof VariableDeclarationExpression) {
      final Type type = ((VariableDeclarationExpression) parent).getType();
      final ITypeBinding typeBinding = type.resolveBinding();
      if (null != typeBinding) {
        addFreeRef(declaration, typeBinding);
      } else {
        warn(declaration, "Cannot resolve type of %s", parent);
      }
    } else if (parent instanceof FieldDeclaration) {
      final ITypeBinding typeBinding = ((FieldDeclaration) parent).getType().resolveBinding();
      if (null != typeBinding) {
        addFreeRef(declaration, typeBinding);
      } else {
        warn(declaration, "Cannot resolve type of %s", parent);
      }
    } else if (parent instanceof LambdaExpression) {
      final LambdaExpression lambdaExpression = (LambdaExpression) parent;
      final IMethodBinding methodBinding = lambdaExpression.resolveMethodBinding();
      final int paramNumber = lambdaExpression.parameters().indexOf(declaration);
      if (methodBinding != null && paramNumber >= 0) {
        addFreeRef(declaration, methodBinding.getParameterTypes()[paramNumber]);
      } else if (paramNumber < 0) {
        warn(declaration, "Cannot argument index of %s", parent);
      } else {
        warn(declaration, "Cannot method of %s", parent);
      }
    } else {
      warn(declaration, "Cannot handle %s", parent);
    }
  }

  @Override
  public void endVisit(@NotNull SingleVariableDeclaration declaration) {
    if (skip(declaration)) return;
    ITypeBinding typeBinding = declaration.resolveBinding().getType();
    if (null == typeBinding) {
      info(declaration, "Cannot add freeRef for field access %s (binding not resolved)", declaration.getName());
    } else {
      addFreeRef(declaration, typeBinding);
    }
  }

  @Override
  public void endVisit(ClassInstanceCreation node) {
    info(node, "Processing constructor: %s", node);
    if (skip(node)) {
      info(node, "Skip");
      return;
    }
    final IMethodBinding methodBinding = node.resolveConstructorBinding();
    if (null != methodBinding) {
      apply(node, methodBinding.getDeclaringClass());
    } else {
      warn(node, "Unresolved constructor binding on %s", node);
    }
  }

  @Override
  public void endVisit(@NotNull MethodInvocation node) {
    if (skip(node)) return;
    if (Arrays.asList("addRef", "addRefs").contains(node.getName().toString())) return;
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null != methodBinding) {
      apply(node, methodBinding.getReturnType());
    } else {
      warn(node, "Unresolved binding on %s", node);
    }
  }

  private Statement getStatement(ASTNode node) {
    if (node == null) return null;
    if (node instanceof Statement) return (Statement) node;
    return getStatement(node.getParent());
  }

  public boolean hasReturnValue(ASTNode lambdaParent, LambdaExpression node) {
    if (lambdaParent instanceof MethodInvocation) {
      final MethodInvocation methodInvocation = (MethodInvocation) lambdaParent;
      final int argIndex = methodInvocation.arguments().indexOf(node);
      final ITypeBinding targetClass = methodInvocation.resolveMethodBinding().getParameterTypes()[argIndex];
      if (AutoCoder.derives(targetClass, Consumer.class)) {
        return false;
      } else if (AutoCoder.derives(targetClass, Function.class)) {
        return true;
      } else if (AutoCoder.derives(targetClass, Predicate.class)) {
        return true;
      } else {
        final List<IMethodBinding> methods = Arrays.stream(targetClass.getDeclaredMethods()).filter(x -> x.getDefaultValue() != null).collect(Collectors.toList());
        if (methods.size() == 1 && (methods.get(0).getReturnType()).equals(PrimitiveType.VOID)) {
          return false;
        } else {
          warn(node, "Cannot determine if %s returns a value", targetClass.getQualifiedName());
        }
      }
    }
    return false;
  }

  @NotNull
  public Consumer<IndexSymbols.Mention> insertAddRef_ComplexReturn(@NotNull SimpleName node, @NotNull ITypeBinding typeBinding) {
    AST ast = node.getAST();
    return mention -> {
      final ReturnStatement returnStatement = (ReturnStatement) mention.statement;
      final String identifier = randomIdentifier(node);
      final List statements = mention.block.statements();
      statements.add(mention.line, AutoCoder.newLocalVariable(identifier, returnStatement.getExpression()));
      ASTNode name1 = ast.newSimpleName(node.getIdentifier());
      statements.add(mention.line + 1, name1.getAST().newExpressionStatement(newFreeRef(name1, typeBinding)));
      final ReturnStatement newReturnStatement = ast.newReturnStatement();
      newReturnStatement.setExpression(ast.newSimpleName(identifier));
      statements.set(mention.line + 2, newReturnStatement);
      info(node, "Added freeRef for return value %s", node);
    };
  }

  @NotNull
  public Consumer<IndexSymbols.Mention> insertFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName name, @NotNull Block body) {
    return lastMention -> {
      body.statements().add(lastMention.line + 1, name.getAST().newExpressionStatement(newFreeRef(name, typeBinding)));
      info(name, "Added freeRef for value %s (%s)", name, typeBinding.getQualifiedName());
    };
  }

  public boolean skip(@NotNull ASTNode node) {
    return AutoCoder.enclosingMethods(node).stream().filter(enclosingMethod -> {
      final String methodName = enclosingMethod.getName();
      return methodName.equals("addRefs") || methodName.equals("freeRefs");
    }).findFirst().isPresent();
  }

}
