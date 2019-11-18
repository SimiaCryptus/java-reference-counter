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

package com.simiacryptus.devutil;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Tuple2;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefAutoCoder extends AutoCoder {

  private boolean verbose = true;
  private boolean addRefcounting = true;
  @NotNull
  private Random random = new Random();

  public RefAutoCoder(@NotNull String pathname) {
    super(pathname);
  }

  @Override
  @Nonnull
  public void rewrite() {
    if (isVerbose()) rewrite((cu, file) -> new LogNodes(cu, file));
    rewrite((cu, file) -> new RemoveRefs(cu, file));
    while (rewrite((cu, file) -> new InlineRefs(cu, file)) > 0) {
      logger.info("Re-running InlineRefs");
    }
    if (isAddRefcounting()) {
      rewrite((cu, file) -> new InsertMethods(cu, file));
      rewrite((cu, file) -> new InsertAddRefs(cu, file));
      rewrite((cu, file) -> new ModifyFieldSets(cu, file));
      rewrite((cu, file) -> new InsertFreeRefs(cu, file));

      final SymbolIndex index = new SymbolIndex();
      scan((cu, file) -> new IndexSymbols(cu, file, index));
      rewrite((cu, file) -> new InstrumentClosures(cu, file, index));
      // Identify closures - warn on lambda, handle anonymous classes
      // Wrap collections and streams in Ref-Aware support classes
      // Optimize adjacent addRef / freeRef
    }
  }

  public boolean isVerbose() {
    return verbose;
  }

  @NotNull
  public RefAutoCoder setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  public boolean isAddRefcounting() {
    return addRefcounting;
  }

  @NotNull
  public RefAutoCoder setAddRefcounting(boolean addRefcounting) {
    this.addRefcounting = addRefcounting;
    return this;
  }

  public boolean isRefCounted(@NotNull ITypeBinding resolveTypeBinding) {
    final ITypeBinding type;
    if (resolveTypeBinding.isArray()) {
      type = resolveTypeBinding.getElementType();
    } else {
      type = resolveTypeBinding;
    }
    return derives(type, ReferenceCountingBase.class);
  }

  protected class LogNodes extends FileAstVisitor {

    private LogNodes(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void preVisit(@NotNull ASTNode node) {
      logger.info(String.format("Previsit: %s at (%s:%s)", node.getClass(), file.getName(), compilationUnit.getLineNumber(node.getStartPosition())));
    }
  }

  protected class ModifyFieldSets extends FileAstVisitor {

    private ModifyFieldSets(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull Assignment node) {
      if (node.getLeftHandSide() instanceof FieldAccess) {
        final FieldAccess fieldAccess = (FieldAccess) node.getLeftHandSide();
        final ASTNode parent = node.getParent();
        if (parent instanceof ExpressionStatement) {
          final ASTNode parent2 = parent.getParent();
          final ITypeBinding typeBinding = fieldAccess.resolveTypeBinding();
          if (parent2 instanceof Block) {
            final Block block = (Block) parent2;
            final int lineNumber = block.statements().indexOf(parent);
            final Expression rightHandSide = node.getRightHandSide();
            final AST ast = node.getAST();
            if (rightHandSide instanceof Name) {
              block.statements().add(lineNumber, freeRefStatement(fieldAccess));
              node.setRightHandSide(wrapAddRef(rightHandSide, typeBinding));
              logger.info("Simple field-set statement at line " + lineNumber);
            } else {
              final Block exchangeBlock = ast.newBlock();

              final String identifier = randomIdentifier("(" + getLocation(node) + ")");
              exchangeBlock.statements().add(newLocalVariable(identifier, rightHandSide, getType(ast, typeBinding.getName())));
              exchangeBlock.statements().add(freeRefStatement(fieldAccess));

              final Assignment assignment = ast.newAssignment();
              assignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, fieldAccess));
              assignment.setOperator(Assignment.Operator.ASSIGN);
              assignment.setRightHandSide(wrapAddRef(ast.newSimpleName(identifier), typeBinding));
              exchangeBlock.statements().add(ast.newExpressionStatement(assignment));

              block.statements().set(lineNumber, exchangeBlock);
              logger.info("Complex field-set statement at line " + lineNumber);
            }
          } else {
            logger.info(String.format("Non-block field-set statement: %s (%s)", parent.getClass(), parent));
          }
        } else {
          logger.info(String.format("Non-ExpressionStatement field-set statement: %s (%s)", parent.getClass(), parent));
        }
      }
      super.endVisit(node);
    }

    @NotNull
    public IfStatement freeRefStatement(@NotNull FieldAccess fieldAccess) {
      AST ast = fieldAccess.getAST();
      final IfStatement ifStatement = ast.newIfStatement();
      final InfixExpression infixExpression = ast.newInfixExpression();
      infixExpression.setLeftOperand(ast.newNullLiteral());
      infixExpression.setRightOperand((Expression) ASTNode.copySubtree(ast, fieldAccess));
      infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
      ifStatement.setExpression(infixExpression);
      final ITypeBinding typeBinding = fieldAccess.resolveTypeBinding();
      if (null == typeBinding) {
        logger.info(String.format("Cannot add freeRef for field access %s at %s (binding not resolved)",
            fieldAccess.getName(),
            "(" + getLocation(fieldAccess) + ")"
        ));
      } else {
        ifStatement.setThenStatement(ast.newExpressionStatement(newFreeRef(fieldAccess, typeBinding)));
        logger.info(String.format("Added freeRef for field access %s at %s",
            fieldAccess.getName(),
            "(" + getLocation(fieldAccess) + ")"
        ));
      }
      return ifStatement;
    }
  }

  protected class RemoveRefs extends FileAstVisitor {

    private RemoveRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull TypeDeclaration node) {
      final ITypeBinding typeBinding = node.resolveBinding();
      if (derives(typeBinding, ReferenceCountingBase.class)) {
        removeMethods(node, "addRef");
        removeMethods(node, "freeRef");
        removeMethods(node, "_free");
        removeMethods(node, "addRefs");
        removeMethods(node, "freeRefs");
      }
      super.endVisit(node);
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
//        replace(node, subject);
        final ASTNode parent = node.getParent();
        if (parent instanceof MethodInvocation) {
          final List arguments = ((MethodInvocation) parent).arguments();
          final int index = arguments.indexOf(node);
          arguments.set(index, subject);
          logger.info(String.format("%s removed as argument %s of %s", methodName, index, parent));
        } else if (parent instanceof ExpressionStatement) {
          delete((ExpressionStatement) parent);
          logger.info(String.format("%s removed from %s", methodName, parent.toString().trim()));
        } else if (parent instanceof ClassInstanceCreation) {
          final List arguments = ((ClassInstanceCreation) parent).arguments();
          final int index = arguments.indexOf(node);
          arguments.set(index, subject);
          logger.info(String.format("%s removed as argument %s of %s", methodName, index, parent));
        } else if (parent instanceof VariableDeclarationFragment) {
          ((VariableDeclarationFragment) parent).setInitializer(subject);
          logger.info(String.format("%s removed at %s", methodName, "(" + getLocation(parent) + ")"));
        } else if (parent instanceof Assignment) {
          ((Assignment) parent).setRightHandSide(subject);
          logger.info(String.format("%s removed at %s", methodName, "(" + getLocation(parent) + ")"));
        } else if (parent instanceof ArrayInitializer) {
          final List arguments = ((ArrayInitializer) parent).expressions();
          final int index = arguments.indexOf(node);
          arguments.set(index, subject);
          logger.info(String.format("%s removed as argument %s of %s", methodName, index, parent));
        } else {
          logger.warn(String.format("%s - Cannot remove %s called in %s: %s", "(" + getLocation(parent) + ")", methodName, parent.getClass(), parent));
        }
      }
    }

  }

  protected class InlineRefs extends FileAstVisitor {

    private InlineRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull Block node) {
      if (node.statements().size() == 1 && node.getParent() instanceof Block) {
        final Block parent = (Block) node.getParent();
        parent.statements().set(parent.statements().indexOf(node),
            ASTNode.copySubtree(node.getAST(), (ASTNode) node.statements().get(0)));
      }
    }

    @Override
    public void endVisit(@NotNull Assignment node) {
      Statement previousStatement = previousStatement(node);
      if (previousStatement != null) {
        if (previousStatement instanceof VariableDeclarationStatement) {
          final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
          if (1 == fragments.size()) {
            final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
            if (fragment.getName().toString().equals(node.getRightHandSide().toString())) {
              logger.info(String.format("Inlining %s at %s", fragment.getName(), "(" + getLocation(node) + ")"));
              node.setRightHandSide((Expression) ASTNode.copySubtree(node.getAST(), fragment.getInitializer()));
              previousStatement.delete();
            } else {
              logger.warn(String.format("%s previous variable %s is not used in %s", "(" + getLocation(node) + ")", fragment.getName(), node.getRightHandSide()));
            }
          } else {
            logger.warn(String.format("%s previous variable has multiple fragments", "(" + getLocation(node) + ")"));
          }
        } else {
          logger.warn(String.format("%s previous statement is %s", "(" + getLocation(node) + ")", previousStatement.getClass().getSimpleName()));
        }
      }
    }

    @Nullable
    public Statement previousStatement(@Nonnull ASTNode node) {
      if (node instanceof Statement) {
        final ASTNode statementParent = node.getParent();
        if (statementParent instanceof Block) {
          final List statements = ((Block) statementParent).statements();
          final int statementNumber = statements.indexOf(node);
          if (statementNumber > 0) {
            return (Statement) statements.get(statementNumber - 1);
          } else {
            //logger.info(String.format("No previous statement for %s at %s", node.getClass().getSimpleName(), location(node)));
            return null;
          }
        } else {
          logger.info(String.format("No previous statement for %s at %s", node.getClass().getSimpleName(), "(" + getLocation(node) + ")"));
          return null;
        }
      } else {
        final ASTNode parent = node.getParent();
        if (null == parent) {
          logger.info("No previous statement for %s at %s", node.getClass().getSimpleName(), "(" + getLocation(node) + ")");
          return null;
        } else {
          return previousStatement(parent);
        }
      }
    }

    @Override
    public void endVisit(@NotNull ReturnStatement node) {
      if (node.getExpression() instanceof Name) {
        Statement previousStatement = previousStatement(node);
        if (previousStatement != null) {
          if (previousStatement instanceof VariableDeclarationStatement) {
            final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
            if (1 == fragments.size()) {
              final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
              if (fragment.getName().toString().equals(node.getExpression().toString())) {
                logger.info(String.format("Inlining %s at %s", fragment.getName(), "(" + getLocation(node) + ")"));
                node.setExpression((Expression) ASTNode.copySubtree(node.getAST(), fragment.getInitializer()));
                previousStatement.delete();
              }
            }
          } else {
            logger.info(String.format("Cannot inline - Previous statement is %s at %s", previousStatement.getClass().getSimpleName(), "(" + getLocation(node) + ")"));
          }
        } else {
          logger.info(String.format("Cannot inline - No previous statement at %s", "(" + getLocation(node) + ")"));
        }
      }
    }
  }

  protected class InstrumentClosures extends FileAstVisitor {

    private final SymbolIndex index;

    private InstrumentClosures(CompilationUnit compilationUnit, File file, SymbolIndex index) {
      super(compilationUnit, file);
      this.index = index;
    }

    @Override
    public void endVisit(LambdaExpression node) {
      final IMethodBinding methodBinding = node.resolveMethodBinding();
      if (null == methodBinding) return;
      final BindingId bindingId = index.describe(methodBinding).setType("Lambda");
      final Span lambdaLocation = getSpan(node);
      final SymbolIndex symbolIndex = getSymbolIndex(node);
      final Map<BindingId, List<Span>> closures = getClosures(lambdaLocation, symbolIndex);
      if (closures.size() > 0) {
        logger.info(String.format("Closures in %s at %s\n\t%s",
            bindingId,
            lambdaLocation,
            closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + "\n\t" + b).get()));

        final AST ast = node.getAST();
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(ast.newSimpleName("ReferenceCounting"));
        methodInvocation.setName(ast.newSimpleName("wrapInterface"));
        methodInvocation.arguments().add(ASTNode.copySubtree(ast, node));
        closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
          if (closureNode instanceof SingleVariableDeclaration) {
            final MethodInvocation addRefInvoke = ast.newMethodInvocation();
            final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
            final ITypeBinding type = singleVariableDeclaration.resolveBinding().getType();
            if (isRefCounted(type)) {
              addRefInvoke.setExpression((Name) ASTNode.copySubtree(ast, singleVariableDeclaration.getName()));
              addRefInvoke.setName(ast.newSimpleName("addRef"));
              methodInvocation.arguments().add(addRefInvoke);
            }
          } else {
            logger.warn("Cannot handle " + closureNode.getClass().getSimpleName());
          }
        });
        if (methodInvocation.arguments().size() > 1) {
          replace(node, methodInvocation);
        }
      }
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
      final Span lambdaLocation = getSpan(node);
      final Map<BindingId, List<Span>> closures = getClosures(lambdaLocation, getSymbolIndex(node));

      if (closures.size() > 0) {
        logger.warn(String.format("Closures in %s at %s: %s",
            index.describe(node.resolveBinding()).setType("Anonymous Class"),
            lambdaLocation,
            closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()));
      }
    }

    private Map<BindingId, List<Span>> getClosures(Span lambdaLocation, SymbolIndex lambdaIndex) {
      return lambdaIndex.references.entrySet().stream().flatMap(e -> {
        final BindingId bindingId = e.getKey();
        final ContextLocation definition = index.definitionLocations.get(bindingId);
        if (bindingId.type.equals("Type")) {
        } else if (definition == null) {
          logger.debug(String.format("Unresolved ref %s in %s", bindingId, lambdaLocation));
        } else if (!lambdaLocation.contains(definition.location)) {
          logger.debug(String.format("Closure %s in %s defined by %s", bindingId, e.getValue().stream().map(x -> x.location.toString()).reduce((a, b) -> a + ", " + b).get(), definition.location));
          return e.getValue().stream().map(x -> new Tuple2<>(bindingId, x.location));
        }
        return Stream.empty();
      }).filter(x -> x != null).collect(Collectors.groupingBy(x -> x._1, Collectors.mapping(x -> x._2, Collectors.toList())));
    }

    @NotNull
    private AutoCoder.SymbolIndex getSymbolIndex(ASTNode node) {
      final SymbolIndex lambdaIndex = new SymbolIndex();
      node.accept(new IndexSymbols(compilationUnit, file, lambdaIndex) {
        @Override
        public void endVisit(QualifiedName node) {
          Name root = node;
          while (root instanceof QualifiedName) {
            root = ((QualifiedName) root).getQualifier();
          }
          final IBinding binding = root.resolveBinding();
          if (null != binding) {
            indexReference(root, binding);
          }
        }
      }.setVerbose(false));
      return lambdaIndex;
    }
  }

  protected class InsertFreeRefs extends FileAstVisitor {

    private InsertFreeRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull VariableDeclarationFragment declaration) {
      final ASTNode parent = declaration.getParent();
      if (parent instanceof VariableDeclarationStatement) {
        final ITypeBinding typeBinding = ((VariableDeclarationStatement) parent).getType().resolveBinding();
        if (null != typeBinding) {
          addFreeRef(declaration, typeBinding);
        } else {
          logger.warn(String.format("%s - Cannot resolve type of %s", "(" + getLocation(parent) + ")", parent));
        }
      } else if (parent instanceof VariableDeclarationExpression) {
        final Type type = ((VariableDeclarationExpression) parent).getType();
        final ITypeBinding typeBinding = type.resolveBinding();
        if (null != typeBinding) {
          addFreeRef(declaration, typeBinding);
        } else {
          logger.warn(String.format("%s - Cannot resolve type of %s", "(" + getLocation(parent) + ")", parent));
        }
      } else if (parent instanceof FieldDeclaration) {
        final ITypeBinding typeBinding = ((FieldDeclaration) parent).getType().resolveBinding();
        if (null != typeBinding) {
          addFreeRef(declaration, typeBinding);
        } else {
          logger.warn(String.format("%s - Cannot resolve type of %s", "(" + getLocation(parent) + ")", parent));
        }
      } else if (parent instanceof LambdaExpression) {
        final LambdaExpression lambdaExpression = (LambdaExpression) parent;
        final IMethodBinding methodBinding = lambdaExpression.resolveMethodBinding();
        final int paramNumber = lambdaExpression.parameters().indexOf(declaration);
        if (methodBinding != null) {
          addFreeRef(declaration, methodBinding.getParameterTypes()[paramNumber]);
        }
      } else {
        logger.warn("Cannot handle " + parent);
      }
    }

    @Override
    public void endVisit(@NotNull SingleVariableDeclaration declaration) {
      ITypeBinding typeBinding = declaration.resolveBinding().getType();
      if (null == typeBinding) {
        logger.info(String.format("Cannot add freeRef for field access %s at %s (binding not resolved)",
            declaration.getName(),
            "(" + getLocation(declaration) + ")"
        ));
      } else {
        addFreeRef(declaration, typeBinding);
      }
    }

    public void addFreeRef(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding) {
      if (derives(typeBinding, ReferenceCountingBase.class)) {
        final SimpleName name = declaration.getName();
        ASTNode parent = declaration.getParent();
        if (parent instanceof MethodDeclaration) {
          final MethodDeclaration node = (MethodDeclaration) parent;
          addFreeRef(typeBinding, name, node.getBody());
        } else if (parent instanceof LambdaExpression) {
          final LambdaExpression node = (LambdaExpression) parent;
          final ASTNode lambdaParent = node.getParent();
          //if (!isStream(lambdaParent))
          {
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
          }
        } else if (parent instanceof VariableDeclarationStatement) {
          parent = parent.getParent();
          if (parent instanceof Block) {
            addFreeRef(typeBinding, name, (Block) parent);
          } else {
            logger.warn(String.format("%s - Cannot add freeRef for %s (VariableDeclarationStatement) in %s : %s",
                "(" + getLocation(declaration) + ")",
                name,
                parent.getClass(), parent.toString().trim()
            ));
          }
        } else if (parent instanceof FieldDeclaration) {
          final ASTNode fieldParent = parent.getParent();
          if (fieldParent instanceof TypeDeclaration) {
            final TypeDeclaration typeDeclaration = (TypeDeclaration) fieldParent;
            final Optional<MethodDeclaration> freeMethodOpt = findMethod(typeDeclaration, "_free");
            if (freeMethodOpt.isPresent()) {
              final ExpressionStatement expressionStatement = name.getAST().newExpressionStatement(newFreeRef(name, typeBinding));
              logger.info(String.format("%s - Adding freeRef for %s::%s to %s",
                  "(" + getLocation(declaration) + ")",
                  typeDeclaration.getName(),
                  declaration.getName(),
                  "(" + getLocation(name) + ")"
              ));
              freeMethodOpt.get().getBody().statements().add(0, expressionStatement);
            } else {
              logger.warn(String.format("%s - Cannot add freeRef for %s::%s - no _free method",
                  "(" + getLocation(declaration) + ")",
                  typeDeclaration.getName(),
                  declaration.getName()
              ));
            }
          } else {
            logger.warn(String.format("%s - Cannot add freeRef for %s (FieldDeclaration) in %s : %s",
                "(" + getLocation(declaration) + ")",
                name,
                fieldParent.getClass(), fieldParent.toString().trim()
            ));
          }
        } else {
          logger.warn(String.format("%s - Cannot add freeRef for %s in %s : %s",
              "(" + getLocation(declaration) + ")",
              name,
              parent.getClass(), parent.toString().trim()
          ));
        }
      }
    }

    public boolean hasReturnValue(ASTNode lambdaParent, LambdaExpression node) {
      if (lambdaParent instanceof MethodInvocation) {
        final MethodInvocation methodInvocation = (MethodInvocation) lambdaParent;
        final int argIndex = methodInvocation.arguments().indexOf(node);
        final ITypeBinding targetClass = methodInvocation.resolveMethodBinding().getParameterTypes()[argIndex];
        if (derives(targetClass, Consumer.class)) {
          return false;
        } else if (derives(targetClass, Function.class)) {
          return true;
        } else if (derives(targetClass, Predicate.class)) {
          return true;
        } else {
          final List<IMethodBinding> methods = Arrays.stream(targetClass.getDeclaredMethods()).filter(x -> x.getDefaultValue() != null).collect(Collectors.toList());
          if (methods.size() == 1 && (methods.get(0).getReturnType()).equals(PrimitiveType.VOID)) {
            return false;
          } else {
            logger.warn(String.format("Cannot determine if %s returns a value", targetClass.getQualifiedName()));
          }
        }
      }
      return false;
    }

    public void addFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName name, @NotNull Block body) {
      final List<Mention> lastMentions = lastMentions(body, name.resolveBinding());
      lastMentions.stream().filter(x -> !x.isReturn())
          .forEach(insertFreeRef(typeBinding, name, body));
      lastMentions.stream().filter(x -> x.isComplexReturn())
          .forEach(insertAddRef_ComplexReturn(name, typeBinding));
    }

    @NotNull
    public Consumer<Mention> insertAddRef_ComplexReturn(@NotNull SimpleName name, @NotNull ITypeBinding typeBinding) {
      AST ast = name.getAST();
      return mention -> {
        final ReturnStatement returnStatement = (ReturnStatement) mention.statement;
        final String identifier = randomIdentifier("(" + getLocation(name) + ")");
        final List statements = mention.block.statements();
        statements.add(mention.line, newLocalVariable(identifier, returnStatement.getExpression()));
        ASTNode name1 = ast.newSimpleName(name.getIdentifier());
        statements.add(mention.line + 1, name1.getAST().newExpressionStatement(newFreeRef(name1, typeBinding)));
        final ReturnStatement newReturnStatement = ast.newReturnStatement();
        newReturnStatement.setExpression(ast.newSimpleName(identifier));
        statements.set(mention.line + 2, newReturnStatement);
        logger.info(String.format("Added freeRef for return value %s at %s",
            name,
            "(" + getLocation(name) + ")"
        ));
      };
    }

    @NotNull
    public Consumer<Mention> insertFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName name, @NotNull Block body) {
      return lastMention -> {
        body.statements().add(lastMention.line + 1, name.getAST().newExpressionStatement(newFreeRef(name, typeBinding)));
        logger.info(String.format("Added freeRef for value %s (%s) at %s",
            name,
            typeBinding.getQualifiedName(),
            "(" + getLocation(name) + ")"
        ));
      };
    }

    @Override
    public void endVisit(@NotNull MethodInvocation node) {
      final IMethodBinding methodBinding = node.resolveMethodBinding();
      if (null != methodBinding) {
        if (modifyArg(methodBinding.getDeclaringClass()) && !node.getName().toString().equals("addRefs")) {
          final List arguments = node.arguments();
          for (int i = 0; i < arguments.size(); i++) {
            Object next = arguments.get(i);
            MethodInvocation methodInvocation = wrapAddRef((ASTNode) next);
            if (null != methodInvocation) {
              final SimpleName nodeName = node.getName();
              logger.info(String.format("Argument addRef for %s: %s at %s",
                  nodeName,
                  nodeName.resolveTypeBinding().getQualifiedName(),
                  "(" + getLocation(nodeName) + ")"));
              arguments.set(i, methodInvocation);
            }
          }
        }
      }
    }

    @Override
    public void endVisit(@NotNull ArrayInitializer node) {
      final ITypeBinding typeBinding = node.resolveTypeBinding();
      if (null != typeBinding) {
        if (modifyArg(typeBinding.getElementType())) {
          final List expressions = node.expressions();
          for (int i = 0; i < expressions.size(); i++) {
            Object next = expressions.get(i);
            MethodInvocation methodInvocation = wrapAddRef((ASTNode) next);
            if (null != methodInvocation) {
              logger.info(String.format("Argument addRef for %s at %s",
                  next,
                  "(" + getLocation(node) + ")"));
              expressions.set(i, methodInvocation);
            }
          }
        }
      }
    }

    @Nullable
    public MethodInvocation wrapAddRef(ASTNode node) {
      if (node instanceof SimpleName) {
        final SimpleName name = (SimpleName) node;
        if (derives(name.resolveTypeBinding(), ReferenceCountingBase.class)) {
          return (MethodInvocation) RefAutoCoder.this.wrapAddRef(name, name.resolveTypeBinding());
        }
      }
      return null;
    }

    public boolean modifyArg(@NotNull ITypeBinding declaringClass) {
      return AutoCoder.toString(declaringClass.getPackage()).startsWith("com.simiacryptus");
    }

  }

  protected class InsertAddRefs extends FileAstVisitor {

    private InsertAddRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull ConstructorInvocation node) {
      final IMethodBinding methodBinding = node.resolveConstructorBinding();
      if (null != methodBinding) {
        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (methodsConsumeRefs(declaringClass) && node.arguments().size() > 0) {
          apply(node, node.arguments());
        }
      } else {
        logger.warn("Cannot resolve " + node);
      }
    }

    @Override
    public void endVisit(@NotNull ClassInstanceCreation node) {
      final IMethodBinding methodBinding = node.resolveConstructorBinding();
      if (null != methodBinding) {
        if (methodsConsumeRefs(methodBinding.getDeclaringClass())) {
          if (node.arguments().size() > 0) {
            apply(node.getType(), node.arguments());
          } else {
            logger.info(String.format("No args %s at %s", node, "(" + getLocation(node) + ")"));
          }
        } else {
          logger.info(String.format("Non-refcounted arg %s at %s", node, "(" + getLocation(node) + ")"));
        }
      } else {
        logger.warn(String.format("Cannot resolve %s at %s", node, "(" + getLocation(node) + ")"));
      }
    }

    @Override
    public void endVisit(@NotNull MethodInvocation node) {
      final IMethodBinding methodBinding = node.resolveMethodBinding();
      if (null != methodBinding) {
        if (methodsConsumeRefs(methodBinding.getDeclaringClass())) {
          apply(node.getName(), node.arguments());
        }
      }
    }

    public void apply(@NotNull ASTNode node, @NotNull List<ASTNode> arguments) {
      for (int i = 0; i < arguments.size(); i++) {
        ASTNode next = arguments.get(i);
        if (next instanceof SimpleName) {
          final SimpleName simpleName = (SimpleName) next;
          final ITypeBinding resolveTypeBinding = simpleName.resolveTypeBinding();
          if (isRefCounted(resolveTypeBinding)) {
            arguments.set(i, addAddRef(simpleName, resolveTypeBinding));
            logger.info(String.format("Argument addRef for %s: %s (%s) defined by %s", node, resolveTypeBinding.getQualifiedName(), simpleName, "(" + getLocation(node) + ")"));
          } else {
            logger.info(String.format("Non-refcounted arg %s at %s", simpleName, "(" + getLocation(simpleName) + ")"));
          }
        }
      }
    }

    @NotNull
    public MethodInvocation addAddRef(@NotNull SimpleName name, @NotNull ITypeBinding type) {
      AST ast = name.getAST();
      if (type.isArray()) {
        final String qualifiedName = type.getElementType().getQualifiedName();
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRefs"));
        methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
        methodInvocation.arguments().add(ast.newSimpleName(name.toString()));
        return methodInvocation;
      } else {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRef"));
        methodInvocation.setExpression(ast.newSimpleName(name.toString()));
        return methodInvocation;
      }
    }

    public boolean methodsConsumeRefs(@NotNull ITypeBinding declaringClass) {
      return AutoCoder.toString(declaringClass.getPackage()).startsWith("com.simiacryptus");
    }

  }

  protected class InsertMethods extends FileAstVisitor {

    public InsertMethods(CompilationUnit cu, File file) {
      super(cu, file);
    }

    @Override
    public void endVisit(@NotNull TypeDeclaration node) {
      if (derives(node.resolveBinding(), ReferenceCountingBase.class)) {
        final AST ast = node.getAST();
        final List declarations = node.bodyDeclarations();
        declarations.add(method_free(ast));
        declarations.add(method_addRef(ast, node.getName()));
        declarations.add(method_addRefs(ast, node.getName()));
        declarations.add(method_freeRefs(ast, node.getName()));
      }
      super.endVisit(node);
    }

    @NotNull
    public MethodDeclaration method_free(@NotNull AST ast) {
      final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
      methodDeclaration.setName(ast.newSimpleName("_free"));
      methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
      methodDeclaration.modifiers().add(annotation_override(ast));
      methodDeclaration.setBody(ast.newBlock());
      return methodDeclaration;
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
        infixExpression.setOperator(InfixExpression.Operator.EQUALS);
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
        infixExpression.setOperator(InfixExpression.Operator.EQUALS);
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

  }

  @NotNull
  public String randomIdentifier(String location) {
    final String id = "temp" + Long.toString(Math.abs(random.nextLong())).substring(0, 4);
    logger.info(String.format("Creating %s at %s", id, location));
    return id;
  }

  @NotNull
  public MethodInvocation newFreeRef(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
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
