package com.simiacryptus.devutil.ref;

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
import java.util.stream.IntStream;
import java.util.stream.Stream;

class InsertFreeRefs extends RefFileAstVisitor {

  InsertFreeRefs(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  public void addFreeRef(@Nonnull ASTNode node, @NotNull Expression expression, @Nonnull ITypeBinding typeBinding) {
    Block block = getBlock(node);
    int lineNumber = findStatementIndex(block, node);
    final AST ast = expression.getAST();
    final String identifier = randomIdentifier(expression);
    final ExpressionStatement localVariable = newLocalVariable(identifier, expression, getType(ast, typeBinding.getQualifiedName()));
    final ExpressionStatement freeStatement = ast.newExpressionStatement(newFreeRef(ast.newSimpleName(identifier), typeBinding));
    replace(expression, ast.newSimpleName(identifier));
    final List statements = block.statements();
    if (lineNumber + 1 < statements.size()) {
      statements.add(lineNumber + 1, freeStatement);
    } else {
      statements.add(freeStatement);
    }
    statements.add(lineNumber, localVariable);
    info(expression, "Wrapped method call with freeref at line %s", lineNumber);
  }

  protected int findStatementIndex(Block block, ASTNode node) {
    final List statements = block.statements();
    return IntStream.range(0, statements.size())
        .filter(i -> contains((ASTNode) statements.get(i), node))
        .findFirst().orElse(-1);
  }

  protected Block getBlock(ASTNode node) {
    ASTNode parent = node.getParent();
    if (parent instanceof MethodDeclaration) {
      final MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
      return methodDeclaration.getBody();
    } else if (parent instanceof LambdaExpression) {
      final LambdaExpression lambdaExpression = (LambdaExpression) parent;
      final ASTNode lambdaParent = lambdaExpression.getParent();
      final ASTNode body = lambdaExpression.getBody();
      if (body instanceof Block) {
        return (Block) body;
      } else {
        final AST ast = lambdaExpression.getAST();
        final Block block = ast.newBlock();
        if (hasReturnValue(lambdaParent, lambdaExpression)) {
          final ReturnStatement returnStatement = ast.newReturnStatement();
          returnStatement.setExpression((Expression) ASTNode.copySubtree(ast, body));
          block.statements().add(returnStatement);
        } else {
          block.statements().add(ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, body)));
        }
        info(lambdaExpression, "Replace lambda %s with block %s", lambdaExpression, block);
        lambdaExpression.setBody(block);
        return block;
      }
    } else if (parent instanceof Block) {
      return (Block) parent;
    } else if (null == parent) {
      warn(node, "Cannot find containing block for %s", node);
      return null;
    } else {
      return getBlock(parent);
    }
  }

  public void addFreeRef(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding) {
    if (skip(declaration)) return;
    if (derives(typeBinding, ReferenceCounting.class)) {
      final SimpleName name = declaration.getName();
      ASTNode parent = declaration.getParent();
      if (parent instanceof MethodDeclaration) {
        final MethodDeclaration node = (MethodDeclaration) parent;
        addFreeRef_variable(typeBinding, name, node.getBody());
      } else if (parent instanceof LambdaExpression) {
        final LambdaExpression node = (LambdaExpression) parent;
        final ASTNode lambdaParent = node.getParent();
        final ASTNode body = node.getBody();
        if (body instanceof Block) {
          addFreeRef_variable(typeBinding, name, (Block) body);
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
          info(node, "Replace lambda %s with block %s", node, block);
          node.setBody(block);
          addFreeRef_variable(typeBinding, name, block);
        }
      } else if (parent instanceof VariableDeclarationStatement) {
        parent = parent.getParent();
        if (parent instanceof Block) {
          addFreeRef_variable(typeBinding, name, (Block) parent);
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

  public void addFreeRef_variable(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, @NotNull Block body) {
    final List<IndexSymbols.Mention> lastMentions = lastMentions(body, node.resolveBinding());
    info(body, "Last mentions of %s: \n\t%s", node, lastMentions.stream().map(x ->
        String.format("%s at line %s (%s)", x.statement.toString().trim(), x.line, x.isReturn() ? "Simple Return" : (x.isComplexReturn() ? "Complex Return" : "Non-return"))
    ).reduce((a, b) -> a + "\n\t" + b).orElse(""));
    if(lastMentions.isEmpty()) {
      insertFreeRef(typeBinding, node, body).accept(new IndexSymbols.Mention(body, -1, null));
    } else {
      lastMentions.stream().filter(x -> !x.isReturn())
          .forEach(insertFreeRef(typeBinding, node, body));
      lastMentions.stream().filter(x -> x.isComplexReturn())
          .forEach(insertAddRef_ComplexReturn(node, typeBinding));
    }
  }

  public void add_freeRef_entry(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding, SimpleName name, ASTNode parent) {
    final ASTNode fieldParent = parent.getParent();
    if (fieldParent instanceof TypeDeclaration) {
      final TypeDeclaration typeDeclaration = (TypeDeclaration) fieldParent;
      final Optional<MethodDeclaration> freeMethodOpt = findMethod(typeDeclaration, "_free");
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

  private void apply(@NotNull Expression node) {
    final ITypeBinding typeBinding = node.resolveTypeBinding();
    if(null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    apply(node, typeBinding);
  }

  private void apply(@NotNull Expression node, ITypeBinding typeBinding) {
    if (isRefCounted(node, typeBinding)) {
      info(node, "Ref-returning method: %s", node);
      final ASTNode parent = node.getParent();
      if (parent instanceof MethodInvocation) {
        final MethodInvocation methodInvocation = (MethodInvocation) parent;
        final IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
        if (null == methodBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
          return;
        }
        if (contains(methodInvocation.getExpression(), node)) {
          if(!methodConsumesSelfRefs(methodBinding)) {
            info(node, "Adding freeref for Ref-returning method: %s", node);
            freeRefs(node, typeBinding);
          } else {
            info(node, "Chained method consumes ref for Ref-returning method: %s", node);
          }
          return;
        }
        if (!methodConsumesRefs(methodBinding, node)) {
          freeRefs(node, typeBinding);
          info(node, "Adding freeref for Ref-returning method: %s", node);
          return;
        }
        info(node, "Result consumed by method");
      } else if (parent instanceof FieldAccess) {
        final FieldAccess methodInvocation = (FieldAccess) parent;
        final IVariableBinding fieldBinding = methodInvocation.resolveFieldBinding();
        if (null == fieldBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
        }
        if (contains(methodInvocation.getExpression(), node)) {
          freeRefs(node, typeBinding);
        } else {
          info(node, "Result consumed by method");
        }
      } else if (parent instanceof ExpressionStatement) {
        final ASTNode statementParent = parent.getParent();
        if (statementParent instanceof Block) {
          final Block block = (Block) statementParent;
          final int lineNumber = block.statements().indexOf(parent);
          block.statements().set(lineNumber, freeRefUtilStatement(node, typeBinding));
          info(node, "Wrapped method call with freeref at line " + lineNumber);
        } else {
          warn(node, "Non-block method call statement: %s (%s)", parent.getClass(), parent);
        }
      } else if (parent instanceof InfixExpression) {
        addFreeRef(parent, node, typeBinding);
        info(node, "Reftype wrapped as local variable: %s (%s)", parent.getClass(), parent);
      } else if (parent instanceof ReturnStatement) {
        info(node, "Reftype returned from method: %s", parent);
      } else if (parent instanceof VariableDeclarationFragment) {
        info(node, "Reftype assigned as local variable: %s", parent);
      } else {
        warn(node, "Non-ExpressionStatement method: %s (%s)", parent.getClass(), parent);
      }
    } else {
      warn(node, "Ignored method returning %s: %s", typeBinding.getQualifiedName(), node);
    }
  }

  @Override
  public void endVisit(@NotNull VariableDeclarationFragment declaration) {
    if (skip(declaration)) return;
    final ASTNode parent = declaration.getParent();
    if (parent instanceof VariableDeclarationStatement) {
      final ITypeBinding typeBinding = ((VariableDeclarationStatement) parent).getType().resolveBinding();
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, typeBinding);
    } else if (parent instanceof VariableDeclarationExpression) {
      final Type type = ((VariableDeclarationExpression) parent).getType();
      final ITypeBinding typeBinding = type.resolveBinding();
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, typeBinding);
    } else if (parent instanceof FieldDeclaration) {
      final ITypeBinding typeBinding = ((FieldDeclaration) parent).getType().resolveBinding();
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, typeBinding);
    } else if (parent instanceof LambdaExpression) {
      final LambdaExpression lambdaExpression = (LambdaExpression) parent;
      final IMethodBinding methodBinding = lambdaExpression.resolveMethodBinding();
      if (methodBinding == null) {
        warn(declaration, "Cannot resolve method of %s", parent);
        return;
      }
      final int paramNumber = lambdaExpression.parameters().indexOf(declaration);
      if (paramNumber < 0) {
        warn(declaration, "Cannot argument index of %s", parent);
        return;
      }
      addFreeRef(declaration, methodBinding.getParameterTypes()[paramNumber]);
    } else {
      warn(declaration, "Cannot handle %s", parent);
    }
  }

//  @Override
//  public void endVisit(InfixExpression node) {
//    if (skip(node)) return;
//    apply(node.getRightOperand());
//    apply(node.getLeftOperand());
//  }

  @Override
  public void endVisit(@NotNull SingleVariableDeclaration declaration) {
    if (skip(declaration)) return;
    final IVariableBinding variableBinding = declaration.resolveBinding();
    if (null == variableBinding) {
      warn(declaration, "Cannot add freeRef for field access %s (binding not resolved)", declaration.getName());
    } else {
      ITypeBinding typeBinding = variableBinding.getType();
      if (null == typeBinding) {
        warn(declaration, "Cannot add freeRef for field access %s (binding not resolved)", declaration.getName());
      } else {
        addFreeRef(declaration, typeBinding);
      }
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
    if (null == methodBinding) {
      warn(node, "Unresolved binding on %s", node);
      return;
    }
    apply(node, methodBinding.getReturnType());
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
      final ExpressionStatement localVariable = newLocalVariable(identifier, node, getType(ast, typeBinding.getQualifiedName()));
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
      final ExpressionStatement localVariable = newLocalVariable(identifier, node, getType(ast, typeBinding.getQualifiedName()));
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
      if (derives(targetClass, Consumer.class)) {
        info(node, "lambda %s has no return value", lambdaParent);
        return false;
      } else if (derives(targetClass, Function.class)) {
        info(node, "lambda %s has return value", lambdaParent);
        return true;
      } else if (derives(targetClass, Predicate.class)) {
        info(node, "lambda %s has return value", lambdaParent);
        return true;
      } else {
        final List<IMethodBinding> methods = allMethods(targetClass)
            .filter(x->(x.getModifiers() & Modifier.STATIC) == 0)
            .filter(x->(x.getModifiers() & Modifier.DEFAULT) != 0)
            .collect(Collectors.toList());
        if (methods.size() == 1) {
          final ITypeBinding returnType = methods.get(0).getReturnType();
          if (returnType.equals(PrimitiveType.VOID)) {
            info(node, "lambda %s has no return value", lambdaParent);
            return false;
          } else {
            info(node, "Lambda interface %s returns a %s value", targetClass.getQualifiedName(), returnType.getQualifiedName());
            return true;
          }
        } else {
          warn(node, "Cannot determine if %s returns a value; it has %s methods", targetClass.getQualifiedName(), methods.size());
        }
      }
    }
    info(node, "lambda %s has no return value", lambdaParent);
    return false;
  }

  @NotNull
  private Stream<IMethodBinding> allMethods(ITypeBinding targetClass) {
    final ITypeBinding superclass = targetClass.getSuperclass();
    Stream<IMethodBinding> declaredMethods = Arrays.stream(targetClass.getDeclaredMethods());
    if(null != superclass && !superclass.getQualifiedName().equals(Object.class.getCanonicalName())) {
      declaredMethods = Stream.concat(declaredMethods, allMethods(superclass));
    }
    declaredMethods = Stream.concat(declaredMethods, Arrays.stream(targetClass.getInterfaces()).flatMap(this::allMethods));
    return declaredMethods.distinct();
  }

  @NotNull
  public Consumer<IndexSymbols.Mention> insertAddRef_ComplexReturn(@NotNull SimpleName node, @NotNull ITypeBinding typeBinding) {
    AST ast = node.getAST();
    return mention -> {
      final ReturnStatement returnStatement = (ReturnStatement) mention.statement;
      final String identifier = randomIdentifier(node);
      final List statements = mention.block.statements();
      statements.add(mention.line, newLocalVariable(identifier, returnStatement.getExpression()));
      ASTNode name1 = ast.newSimpleName(node.getIdentifier());
      statements.add(mention.line + 1, name1.getAST().newExpressionStatement(newFreeRef(name1, typeBinding)));
      final ReturnStatement newReturnStatement = ast.newReturnStatement();
      newReturnStatement.setExpression(ast.newSimpleName(identifier));
      statements.set(mention.line + 2, newReturnStatement);
      info(node, "Added freeRef for return value %s", node);
    };
  }

  @NotNull
  public Consumer<IndexSymbols.Mention> insertFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, @NotNull Block body) {
    return lastMention -> {
      if(lastMention.line < 0) {
        body.statements().add(0, node.getAST().newExpressionStatement(newFreeRef(node, typeBinding)));
      } else {
        body.statements().add(lastMention.line + 1, node.getAST().newExpressionStatement(newFreeRef(node, typeBinding)));
      }
      info(node, "Added freeRef for value %s (%s)", node, typeBinding.getQualifiedName());
    };
  }

}
