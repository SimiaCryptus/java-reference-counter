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

import com.simiacryptus.ref.core.ops.StatementOfInterest;
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

public class InsertFreeRefs extends RefFileAstVisitor {

  public InsertFreeRefs(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  public void addFreeRef(@Nonnull ASTNode node, @NotNull Expression expression, @Nonnull ITypeBinding typeBinding) {
    Block block = getBlock(node);
    int lineNumber = findStatementIndex(block, node);
    final AST ast = expression.getAST();
    final String identifier = randomIdentifier(expression);
    final ExpressionStatement localVariable = newLocalVariable(identifier, expression, getType(node, typeBinding.getQualifiedName()));
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

  public void addFreeRef(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding) {
    if (skip(declaration)) return;
    if (null == typeBinding) return;
    debug(declaration, "addFreeRef: %s", declaration);
    if (isRefCounted(declaration, typeBinding)) {
      final SimpleName name = declaration.getName();
      ASTNode parent = declaration.getParent();
      if (parent instanceof MethodDeclaration) {
        final MethodDeclaration node = (MethodDeclaration) parent;
        insertFreeRefs(typeBinding, name, node.getBody(), 0);
      } else if (parent instanceof LambdaExpression) {
        final LambdaExpression node = (LambdaExpression) parent;
        final ASTNode lambdaParent = node.getParent();
        final ASTNode body = node.getBody();
        if (body instanceof Block) {
          insertFreeRefs(typeBinding, name, (Block) body, 0);
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
          insertFreeRefs(typeBinding, name, block, 0);
        }
      } else if (parent instanceof VariableDeclarationStatement) {
        final ASTNode parentParent = parent.getParent();
        if (parentParent instanceof Block) {
          final Block parentBlock = (Block) parentParent;
          insertFreeRefs(typeBinding, name, parentBlock, parentBlock.statements().indexOf(parent));
        } else {
          warn(declaration, "Cannot add freeRef for %s (VariableDeclarationStatement) in %s : %s", name, parentParent.getClass(), parentParent.toString().trim());
        }
      } else if (parent instanceof FieldDeclaration) {
        add_freeRef_entry(declaration, typeBinding, name, parent);
      } else {
        warn(declaration, "Cannot add freeRef for %s in %s : %s",
            name,
            parent.getClass(), parent.toString().trim());
      }
    } else {
      info(declaration, "%s is not refcounted (%s)", declaration, typeBinding);
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

  @NotNull
  private Stream<IMethodBinding> allMethods(ITypeBinding targetClass) {
    final ITypeBinding superclass = targetClass.getSuperclass();
    Stream<IMethodBinding> declaredMethods = Arrays.stream(targetClass.getDeclaredMethods());
    if (null != superclass && !superclass.getQualifiedName().equals(Object.class.getCanonicalName())) {
      declaredMethods = Stream.concat(declaredMethods, allMethods(superclass));
    }
    declaredMethods = Stream.concat(declaredMethods, Arrays.stream(targetClass.getInterfaces()).flatMap(this::allMethods));
    return declaredMethods.distinct();
  }

  private void apply(@NotNull Expression node) {
    final ITypeBinding typeBinding = node.resolveTypeBinding();
    if (null == typeBinding) {
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
        final Expression expression = methodInvocation.getExpression();
        if (contains(expression, node)) {
          if (!methodConsumesSelfRefs(methodBinding)) {
            info(node, "Adding freeref for Ref-returning method: %s", node);
            freeRefs(node, typeBinding);
          } else {
            info(node, "Chained method consumes ref for Ref-returning method: %s", node);
          }
          return;
        }
        if (!consumesRefs(methodBinding, null==expression?null:expression.resolveTypeBinding())) {
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
    debug(declaration, "VariableDeclarationFragment: %s", declaration);
    final ASTNode parent = declaration.getParent();
    final ITypeBinding declarationType = getTypeBinding(declaration);
    if(null == declarationType) {
      warn(declaration, "Cannot resolve type of %s", declaration);
      return;
    }
    if (parent instanceof VariableDeclarationStatement) {
      final ITypeBinding typeBinding = ((VariableDeclarationStatement) parent).getType().resolveBinding();
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else if (parent instanceof VariableDeclarationExpression) {
      final Type type = ((VariableDeclarationExpression) parent).getType();
      final ITypeBinding typeBinding = type.resolveBinding();
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else if (parent instanceof FieldDeclaration) {
      final ITypeBinding typeBinding = ((FieldDeclaration) parent).getType().resolveBinding();
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else if (parent instanceof LambdaExpression) {
      final LambdaExpression lambdaExpression = (LambdaExpression) parent;
      final IMethodBinding methodBinding = lambdaExpression.resolveMethodBinding();
      if (methodBinding == null) {
        warn(declaration, "Cannot resolve method of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else {
      warn(declaration, "Cannot handle %s", parent);
    }
  }

  @Override
  public void endVisit(@NotNull SingleVariableDeclaration declaration) {
    if (skip(declaration)) return;
    info(declaration, "SingleVariableDeclaration: %s", declaration);
    final IVariableBinding variableBinding = declaration.resolveBinding();
    if (null == variableBinding) {
      warn(declaration, "Cannot add freeRef for field access %s (binding not resolved)", declaration.getName());
    } else {
      addFreeRef(declaration, getTypeBinding(declaration));
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
    if (null == methodBinding) {
      warn(node, "Unresolved constructor binding on %s", node);
      return;
    }
    apply(node, methodBinding.getDeclaringClass());
  }

//  @Override
//  public void endVisit(InfixExpression node) {
//    if (skip(node)) return;
//    apply(node.getRightOperand());
//    apply(node.getLeftOperand());
//  }

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

  protected int findStatementIndex(Block block, ASTNode node) {
    return getLineNumber(block, node);
  }

  private void freeRefs(@NotNull Expression node, ITypeBinding typeBinding) {
    Statement statement = getStatement(node);
    final ASTNode statementParent = statement.getParent();
    AST ast = node.getAST();
    if (statementParent instanceof Block) {
      Block block = (Block) statementParent;
      final List statements = block.statements();
      final int lineNumber = statements.indexOf(statement);
      final String identifier = randomIdentifier(node);
      final ExpressionStatement localVariable = newLocalVariable(identifier, node, getType(node, typeBinding.getQualifiedName()));
      final ExpressionStatement freeStatement = ast.newExpressionStatement(newFreeRef(ast.newSimpleName(identifier), typeBinding));
      replace(node, ast.newSimpleName(identifier));
      if (lineNumber + 1 < statements.size()) {
        statements.add(lineNumber + 1, freeStatement);
      } else {
        statements.add(lineNumber + 1, freeStatement);
      }
      statements.add(lineNumber, localVariable);
      info(node, "Wrapped method call with freeref at line %s", lineNumber);
    } else {
      //warn(node, "Non-block statement %s", statementParent.getClass().getSimpleName());
      Block block = ast.newBlock();
      final List statements = block.statements();
      final String identifier = randomIdentifier(node);
      final ExpressionStatement localVariable = newLocalVariable(identifier, node, getType(node, typeBinding.getQualifiedName()));
      final ExpressionStatement freeStatement = ast.newExpressionStatement(newFreeRef(ast.newSimpleName(identifier), typeBinding));
      replace(node, ast.newSimpleName(identifier));
      statements.add(localVariable);
      statements.add(ASTNode.copySubtree(ast, statement));
      statements.add(freeStatement);
      replace(node, ast.newSimpleName(identifier));
      replace(statement, block);
      info(node, "Wrapped method call with freeref and new block");
    }
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
            .filter(x -> (x.getModifiers() & Modifier.STATIC) == 0)
            .filter(x -> (x.getModifiers() & Modifier.DEFAULT) != 0)
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

  public void insertFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, Block body, int line) {
    final ExpressionStatement statement = body.getAST().newExpressionStatement(newFreeRef(node, typeBinding));
    if (line < 0) {
      body.statements().add(0, statement);
    } else {
      body.statements().add(line + 1, statement);
    }
    info(body, "Added freeRef for value %s (%s) at line %s", node, typeBinding.getQualifiedName(), line);
  }

  private void insertFreeRef_ComplexReturn(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, Block block, int line) {
    ReturnStatement returnStatement = (ReturnStatement) block.statements().get(line);
    AST ast = node.getAST();
    final String identifier = randomIdentifier(node);
    final List statements = block.statements();
    statements.add(line, newLocalVariable(identifier, returnStatement.getExpression()));
    ASTNode name1 = ast.newSimpleName(node.getIdentifier());
    statements.add(line + 1, name1.getAST().newExpressionStatement(newFreeRef(name1, typeBinding)));
    final ReturnStatement newReturnStatement = ast.newReturnStatement();
    newReturnStatement.setExpression(ast.newSimpleName(identifier));
    statements.set(line + 2, newReturnStatement);
    info(node, "Added freeRef for return value %s", node);
  }

  public void insertFreeRefs(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, @NotNull Block body, int declaredAt) {
    final StatementOfInterest lastMention = lastMention(body, node, declaredAt);
    if (null == lastMention) {
      info(node, "No mentions in body. Adding freeRef");
      insertFreeRef(typeBinding, node, body, -1);
    } else {
      if (lastMention.isComplexReturn()) {
        insertFreeRef_ComplexReturn(typeBinding, node, lastMention.block, lastMention.line);
      } else if (!lastMention.isReturn()) {
        info(node, "Adding freeRef after last reference at %s", lastMention.line);
        insertFreeRef(typeBinding, node, lastMention.block, lastMention.line);
      } else {
        info(node, "Last usage returns reference");
      }
      for (StatementOfInterest exitPoint : exits(body, node, declaredAt, lastMention.line)) {
        if (exitPoint.isReturn()) {
          final ReturnStatement returnStatement = (ReturnStatement) exitPoint.statement;
          final Expression expression = returnStatement.getExpression();
          if (null != expression) {
            if (expression.toString().equals(node.toString())) {
              info(node, "Last usage returns reference");
              continue;
            } else if (contains(returnStatement, node)) {
              insertFreeRef_ComplexReturn(typeBinding, node, exitPoint.block, exitPoint.line);
              info(node, "Last usage returns reference");
              continue;
            }
          }
        }
        info(node, "Adding freeRef before flow return at %s", exitPoint.line - 1);
        insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1);
      }
    }
  }

}
