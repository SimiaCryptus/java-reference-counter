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

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.lang.RefCoderIgnore;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@RefCoderIgnore
public abstract class FileAstVisitor extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(FileAstVisitor.class);
  @NotNull
  private static final Random random = new Random();
  protected final CompilationUnit compilationUnit;
  protected final File file;

  protected FileAstVisitor(@Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this.compilationUnit = compilationUnit;
    this.file = file;
  }

  protected static <T> T getField(Object obj, String name) {
    final Field value = Arrays.stream(obj.getClass().getDeclaredFields()).filter(x -> x.getName().equals(name)).findFirst().orElse(null);
    if (value != null) {
      value.setAccessible(true);
      try {
        return (T) value.get(obj);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  protected static boolean derives(@Nonnull ITypeBinding typeBinding, @Nonnull Class<?> baseClass) {
    final String binaryName = typeBinding.getTypeDeclaration().getQualifiedName();
    if (null != binaryName && binaryName.equals(baseClass.getCanonicalName())) return true;
    if (Arrays.stream(typeBinding.getInterfaces()).filter(x -> derives(x, baseClass)).findAny().isPresent()) return true;
    if (typeBinding.getSuperclass() != null) return derives(typeBinding.getSuperclass(), baseClass);
    if (typeBinding.isArray()) return derives(typeBinding.getElementType(), baseClass);
    return false;
  }

  @NotNull
  protected static Name newQualifiedName(AST ast, Class<?> clazz) {
    return newQualifiedName(ast, clazz.getName().split("\\."));
  }

  @NotNull
  protected static Name newQualifiedName(@NotNull AST ast, @NotNull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(i -> new String[i])), simpleName);
  }

  @NotNull
  protected static ArrayType arrayType(@NotNull AST ast, @NotNull String fqTypeName, int rank) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)), rank);
  }

  @NotNull
  protected static MarkerAnnotation annotation_override(@NotNull AST ast) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName("Override"));
    return annotation;
  }

  public static boolean hasAnnotation(IBinding declaringClass, Class<?> aClass) {
    return Arrays.stream(declaringClass.getAnnotations())
        .map(annotation -> annotation.getAnnotationType().getQualifiedName())
        .anyMatch(qualifiedName -> qualifiedName.equals(aClass.getCanonicalName()));
  }

  protected boolean contains(@Nonnull ASTNode expression, @Nonnull IBinding variableBinding) {
    final AtomicBoolean found = new AtomicBoolean(false);
    expression.accept(new ASTVisitor() {
      @Override
      public void endVisit(@NotNull SimpleName node) {
        final IBinding binding = node.resolveBinding();
        if (null != binding && binding.equals(variableBinding)) found.set(true);
      }
    });
    return found.get();
  }

  protected boolean contains(ASTNode node, ASTNode element) {
    if (null == element) return false;
    if (node == element) return true;
    return contains(node, element.getParent());
  }

  protected boolean containsStr(ASTNode node, ASTNode element) {
    if (null == element) return false;
    if (node.toString().equals(element.toString())) return true;
    return contains(node, element.getParent());
  }

  protected void debug(ASTNode node, String formatString, Object... args) {
    debug(1, node, formatString, args);
  }

  protected void debug(int frames, ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.debug(String.format(getFormatString(node, formatString, caller), args));
  }

  protected void delete(@NotNull Statement statement) {
    info(statement, "Deleting %s", statement);
    final ASTNode parent = statement.getParent();
    final AST ast = statement.getAST();
    if (parent instanceof Block) {
      final Block block = (Block) parent;
      if (block.statements().size() == 1) {
        final ASTNode blockParent = block.getParent();
        if (blockParent instanceof LambdaExpression) {
          info(statement, "Keeping block; removing single statement %s", statement);
        } else if (blockParent instanceof MethodDeclaration) {
          info(statement, "Keeping block; removing single statement %s", statement);
        } else if (blockParent instanceof TryStatement) {
          final TryStatement tryStatement = (TryStatement) blockParent;
          final Block tryStatementFinally = tryStatement.getFinally();
          if (null != tryStatementFinally && block.equals(tryStatementFinally) && tryStatement.catchClauses().isEmpty()) {
            info(statement, "Unwrapping try statement %s", tryStatement);
            replace(tryStatement, ASTNode.copySubtree(ast, tryStatement.getBody()));
            return;
          }
        } else {
          delete(block);
          return;
        }
      }
    } else if (parent instanceof Initializer) {
      parent.delete();
      return;
    } else if (parent instanceof EnhancedForStatement) {
      final EnhancedForStatement forStatement = (EnhancedForStatement) parent;
      final List<Statement> evaluableStatements = getEvaluableStatements(forStatement.getExpression());
      if (evaluableStatements.size() == 0) {
        info(statement, "Removing for %s", forStatement);
        delete(forStatement);
      } else if (evaluableStatements.size() == 1) {
        info(statement, "Removing for %s; keeping %s", forStatement, evaluableStatements.get(0));
        replace(forStatement, evaluableStatements.get(0));
      } else {
        final Block block = ast.newBlock();
        block.statements().addAll(evaluableStatements);
        info(statement, "Removing for %s; keeping %s", forStatement, block);
        replace(forStatement, block);
      }
      return;
    } else if (parent instanceof ForStatement) {
      final ForStatement forStatement = (ForStatement) parent;
      final List<Statement> evaluableStatements = getEvaluableStatements(forStatement.getExpression());
      if (evaluableStatements.size() == 0) {
        info(statement, "Removing for %s", forStatement);
        delete(forStatement);
      } else if (evaluableStatements.size() == 1) {
        info(statement, "Removing for %s; keeping %s", forStatement, evaluableStatements.get(0));
        replace(forStatement, evaluableStatements.get(0));
      } else {
        final Block block = ast.newBlock();
        block.statements().addAll(evaluableStatements);
        info(statement, "Removing for %s; keeping %s", forStatement, block);
        replace(forStatement, block);
      }
      return;
    } else if (parent instanceof IfStatement) {
      final IfStatement ifStatement = (IfStatement) parent;
      final Statement elseStatement = ifStatement.getElseStatement();
      final Statement thenStatement = ifStatement.getThenStatement();
      if (thenStatement.equals(statement)) {
        final Expression condition = ifStatement.getExpression();
        if (null != elseStatement) {
          info(statement, "Deleting if-then; inverting %s", ifStatement);
          ifStatement.setElseStatement(null);
          ifStatement.setThenStatement((Statement) ASTNode.copySubtree(ast, elseStatement));
          final PrefixExpression prefixExpression = ast.newPrefixExpression();
          prefixExpression.setOperand((Expression) ASTNode.copySubtree(ast, condition));
          prefixExpression.setOperator(PrefixExpression.Operator.NOT);
          ifStatement.setExpression(prefixExpression);
          return;
        } else {
          final List<Statement> evaluableStatements = getEvaluableStatements(condition);
          if (evaluableStatements.size() == 0) {
            info(statement, "Removing if-then %s", ifStatement);
            delete(ifStatement);
          } else if (evaluableStatements.size() == 1) {
            info(statement, "Removing if-then %s; keeping %s", ifStatement, evaluableStatements.get(0));
            replace(ifStatement, evaluableStatements.get(0));
          } else {
            final Block block = ast.newBlock();
            block.statements().addAll(evaluableStatements);
            info(statement, "Removing if-then %s; keeping %s", ifStatement, block);
            replace(ifStatement, block);
          }
          return;
        }
      } else if (null != elseStatement && elseStatement.equals(statement)) {
        info(statement, "Removing if-else %s", ifStatement);
        ifStatement.setElseStatement(null);
        return;
      } else {
        warn(statement, "Not sure how to remove %s from %s", statement, ifStatement);
      }
    }
    statement.delete();
  }

  protected List<IMethodBinding> enclosingMethods(ASTNode node) {
    final ArrayList<IMethodBinding> list = new ArrayList<>();
    if (null != node) {
      if (node instanceof MethodDeclaration) {
        list.addAll(enclosingMethods(node.getParent()));
        final IMethodBinding methodBinding = ((MethodDeclaration) node).resolveBinding();
        if (null != methodBinding) list.add(methodBinding);
      } else if (null != node.getParent()) {
        list.addAll(enclosingMethods(node.getParent()));
      }
    }
    return list;
  }

  protected List<StatementOfInterest> exits(@NotNull Block block, SimpleName variable, int startAt, int endAt) {
    IBinding binding = variable.resolveBinding();
    if (null == binding) {
      warn(variable, "Unresolved binding");
      return Arrays.asList();
    } else {
      final List<StatementOfInterest> exits = exits(block, startAt, endAt);
      info(variable, "Last mentions of %s: \n\t%s", variable, exits.stream().map(x ->
          String.format("%s at line %s", x.statement.toString().trim(), x.line)
      ).reduce((a, b) -> a + "\n\t" + b).orElse(""));
      return exits;
    }
  }

  private List<StatementOfInterest> exits(@NotNull Block block, int startAt, int endAt) {
    final List statements = block.statements();
    final ArrayList<StatementOfInterest> exits = new ArrayList<>();
    StatementOfInterest lastMention = null;
    final AST ast = block.getAST();
    for (int j = startAt; j < endAt; j++) {
      final Statement statement = (Statement) statements.get(j);
      if (statement instanceof IfStatement) {
        final IfStatement ifStatement = (IfStatement) statement;
        exits.addAll(exits(ast, ifStatement::getThenStatement, ifStatement::setThenStatement));
        exits.addAll(exits(ast, ifStatement::getElseStatement, ifStatement::setElseStatement));
      } else if (statement instanceof WhileStatement) {
        final WhileStatement whileStatement = (WhileStatement) statement;
        exits.addAll(exits(ast, whileStatement::getBody, whileStatement::setBody));
      } else if (statement instanceof DoStatement) {
        final DoStatement doStatement = (DoStatement) statement;
        exits.addAll(exits(ast, doStatement::getBody, doStatement::setBody));
      } else if (statement instanceof ForStatement) {
        final ForStatement forStatement = (ForStatement) statement;
        exits.addAll(exits(ast, forStatement::getBody, forStatement::setBody));
      } else if (statement instanceof EnhancedForStatement) {
        final EnhancedForStatement forStatement = (EnhancedForStatement) statement;
        exits.addAll(exits(ast, forStatement::getBody, forStatement::setBody));
      } else if (statement instanceof TryStatement) {
        final TryStatement tryStatement = (TryStatement) statement;
        exits.addAll(exits(ast, tryStatement::getBody, tryStatement::setBody));
      } else if (statement instanceof SynchronizedStatement) {
        final SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
        exits.addAll(exits(ast, synchronizedStatement::getBody, synchronizedStatement::setBody));
      } else if (isExit(statement)) {
        exits.add(new StatementOfInterest(statement, j));
      }
    }
    if (null != lastMention) exits.add(lastMention);
    return exits;
  }

  private List<StatementOfInterest> exits(AST ast, Supplier<Statement> supplier, Consumer<Block> consumer) {
    Statement body = supplier.get();
    if (!(body instanceof Block) && isExit(body)) {
      final Block newBlock = ast.newBlock();
      newBlock.statements().add(ASTNode.copySubtree(ast, body));
      consumer.accept(newBlock);
      body = newBlock;
    }
    final List<StatementOfInterest> exits1;
    if (body instanceof Block) {
      final Block thenBlock = (Block) body;
      exits1 = exits(thenBlock, 0, (thenBlock).statements().size());
    } else {
      exits1 = new ArrayList<>();
    }
    return exits1;
  }

  protected Optional<MethodDeclaration> findMethod(@NotNull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  protected Optional<MethodDeclaration> findMethod(@NotNull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x -> x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration) methodDeclaration).getName().toString().equals(name)).findFirst();
  }

  @Nullable
  protected Block getBlock(ASTNode node) {
    final ASTNode parent = node.getParent();
    if (parent == null) {
      return null;
    } else if (parent instanceof Block) {
      return (Block) parent;
    } else if (parent instanceof MethodDeclaration) {
      return null;
    } else if (parent instanceof LambdaExpression) {
      return null;
    } else if (parent instanceof TypeDeclaration) {
      return null;
    } else {
      return getBlock(parent);
    }
  }

  protected List<Statement> getEvaluableStatements(Expression expression) {
    final ArrayList<Statement> statements = new ArrayList<>();
    if (isEvaluable(expression)) {
      if (expression instanceof InfixExpression) {
        final InfixExpression infixExpression = (InfixExpression) expression;
        if (isEvaluable(infixExpression.getLeftOperand())) {
          statements.addAll(getEvaluableStatements(infixExpression.getLeftOperand()));
        }
        if (isEvaluable(infixExpression.getRightOperand())) {
          statements.addAll(getEvaluableStatements(infixExpression.getRightOperand()));
        }
      } else if (expression instanceof PrefixExpression) {
        final PrefixExpression prefixExpression = (PrefixExpression) expression;
        if (isEvaluable(prefixExpression.getOperand())) {
          statements.addAll(getEvaluableStatements(prefixExpression.getOperand()));
        }
      } else if (expression instanceof PostfixExpression) {
        final PostfixExpression postfixExpression = (PostfixExpression) expression;
        if (isEvaluable(postfixExpression.getOperand())) {
          statements.addAll(getEvaluableStatements(postfixExpression.getOperand()));
        }
      } else if (expression instanceof ParenthesizedExpression) {
        final ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
        if (isEvaluable(parenthesizedExpression.getExpression())) {
          statements.addAll(getEvaluableStatements(parenthesizedExpression.getExpression()));
        }
      } else if (expression instanceof CastExpression) {
        final CastExpression castExpression = (CastExpression) expression;
        if (isEvaluable(castExpression.getExpression())) {
          statements.addAll(getEvaluableStatements(castExpression.getExpression()));
        }
      } else if (expression instanceof InstanceofExpression) {
        final InstanceofExpression castExpression = (InstanceofExpression) expression;
        if (isEvaluable(castExpression.getLeftOperand())) {
          statements.addAll(getEvaluableStatements(castExpression.getLeftOperand()));
        }
      } else {
        final AST ast = expression.getAST();
        statements.add(ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, expression)));
      }
    }
    return statements;
  }

  protected String getFormatString(@Nonnull ASTNode node, @Nonnull String formatString, @Nonnull StackTraceElement caller) {
    return String.format("(%s) (%s) - %s", toString(caller), getLocation(node), formatString);
  }

  protected int getLineNumber(Block block, ASTNode node) {
    final List statements = block.statements();
    return IntStream.range(0, statements.size()).filter(i -> contains((ASTNode) statements.get(i), node)).findFirst().orElse(-1);
  }

  @NotNull
  protected String getLocation(@Nonnull ASTNode node) {
    final int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
    return file.getName() + ":" + lineNumber;
  }

  @NotNull
  protected IndexSymbols.Span getSpan(ASTNode node) {
    final int startPosition = node.getStartPosition();
    final int length = node.getLength();

    return new IndexSymbols.Span(
        file,
        compilationUnit.getLineNumber(startPosition),
        compilationUnit.getColumnNumber(startPosition),
        compilationUnit.getLineNumber(startPosition + length),
        compilationUnit.getColumnNumber(startPosition + length)
    );
  }

  @NotNull
  protected IndexSymbols.SymbolIndex getSymbolIndex(ASTNode node) {
    final IndexSymbols.SymbolIndex lambdaIndex = new IndexSymbols.SymbolIndex();
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

  @Nullable
  private ArrayList<Integer> getTopTypeDelimiters(ASTNode node, String typeString) {
    int nesting = 0;
    ArrayList<Integer> delimiters = new ArrayList<>();
    for (int i = 0; i < typeString.length(); i++) {
      final char c = typeString.charAt(i);
      if (c == '<') nesting++;
      if (c == '>') nesting--;
      if (c == ',' && nesting == 0) delimiters.add(i);
    }
    if (nesting != 0) {
      warn(node, "Unbalanced type args: %s", typeString);
      return null;
    }
    delimiters.add(typeString.length());
    return delimiters;
  }

  protected Type getType(ASTNode node, @NotNull String name, boolean isDeclaration) {
    @Nonnull AST ast = node.getAST();
    if (name.endsWith("[]")) {
      int rank = 0;
      while (name.endsWith("[]")) {
        name = name.substring(0, name.length() - 2);
        rank++;
      }
      final ArrayType arrayType = ast.newArrayType(getType(node, name, isDeclaration), rank);
      info(node, "Converted type string %s to %s", name, arrayType);
      return arrayType;
    } else if (name.isEmpty()) {
      final WildcardType wildcardType = ast.newWildcardType();
      info(node, "Cannot determine type of %s", node);
      return null;
    } else if (name.equals("?")) {
      final WildcardType wildcardType = ast.newWildcardType();
      info(node, "Converted type string %s to %s", name, wildcardType);
      return wildcardType;
    } else if (name.contains("\\.")) {
      final SimpleType simpleType = ast.newSimpleType(newQualifiedName(ast, name.split("\\.")));
      info(node, "Converted type string %s to %s", name, simpleType);
      return simpleType;
    } else {
      final PrimitiveType.Code typeCode = PrimitiveType.toCode(name);
      if (null != typeCode) {
        final PrimitiveType primitiveType = ast.newPrimitiveType(typeCode);
        info(node, "Converted type string %s to %s", name, primitiveType);
        return primitiveType;
      } else {
        final int typeArg = name.indexOf('<');
        if (typeArg < 0) {
          final SimpleType simpleType = ast.newSimpleType(newQualifiedName(ast, name.split("\\.")));
          info(node, "Converted type string %s to %s", name, simpleType);
          return simpleType;
        } else {
          if (!name.endsWith(">")) {
            warn(node, "Unclosed type args: %s", name);
            return null;
          }
          final String mainType = name.substring(0, typeArg);
          final String innerType = name.substring(typeArg + 1, name.length() - 1);
          ArrayList<Integer> delimiters = getTopTypeDelimiters(node, innerType);
          if (delimiters == null) return null;
          final ArrayList<Type> innerTypes = new ArrayList<>();
          for (int i = 0; i < delimiters.size(); i++) {
            final int to = delimiters.get(i);
            final int from = i == 0 ? 0 : (delimiters.get(i - 1) + 1);
            final String substring = innerType.substring(from, to);
            if (substring.isEmpty()) {
              if (isDeclaration) {
                innerTypes.add(ast.newWildcardType());
              }
              continue;
            }
            innerTypes.add(getTypeParameter(node, substring, isDeclaration));
          }
          final ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(newQualifiedName(ast, mainType.split("\\."))));
          parameterizedType.typeArguments().addAll(innerTypes);
          info(node, "Converted type string %s to %s", name, parameterizedType);
          return parameterizedType;
        }
      }
    }
  }

  protected Type getType(@NotNull Expression node, boolean isDeclaration) {
    final ITypeBinding typeBinding = node.resolveTypeBinding();
    if (null == typeBinding) {
      warn(1, node, "Unresolved binding");
      return null;
    }
    final String qualifiedName = typeBinding.getQualifiedName();
    if (qualifiedName.isEmpty()) {
      warn(1, node, "No type string for %s", typeBinding);
      return null;
    }
    return getType(node, qualifiedName, isDeclaration);
  }

  private Type getTypeParameter(ASTNode node, String name, boolean isDeclaration) {
    final AST ast = node.getAST();
    final String extendsPrefix = "? extends ";
    final String superPrefix = "? super ";
    if (name.startsWith(extendsPrefix)) {
      final WildcardType wildcardType = ast.newWildcardType();
      wildcardType.setBound(getType(node, name.substring(extendsPrefix.length()), isDeclaration));
      wildcardType.setUpperBound(true);
      return wildcardType;
    } else if (name.startsWith(superPrefix)) {
      final WildcardType wildcardType = ast.newWildcardType();
      wildcardType.setBound(getType(node, name.substring(superPrefix.length()), isDeclaration));
      wildcardType.setUpperBound(false);
      return wildcardType;
    } else {
      return getType(node, name, isDeclaration);
    }
  }

  protected void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  protected void info(int frames, @Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getFormatString(node, formatString, caller), Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  protected boolean isEvaluable(Expression node) {
    if (node == null) {
      return false;
    } else if (node instanceof Name) {
      return false;
    } else if (node instanceof ThisExpression) {
      return false;
    } else if (node instanceof NullLiteral) {
      return false;
    } else if (node instanceof CharacterLiteral) {
      return false;
    } else if (node instanceof StringLiteral) {
      return false;
    } else if (node instanceof FieldAccess) {
      return false;
    } else if (node instanceof CastExpression) {
      return isEvaluable(((CastExpression) node).getExpression());
    } else if (node instanceof ParenthesizedExpression) {
      return isEvaluable(((ParenthesizedExpression) node).getExpression());
    } else if (node instanceof InstanceofExpression) {
      return isEvaluable(((InstanceofExpression) node).getLeftOperand());
    } else {
      return true;
    }
  }

  private boolean isExit(Statement statement) {
    if (statement instanceof ReturnStatement) return true;
    if (statement instanceof ThrowStatement) return true;
    return false;
  }

  protected StatementOfInterest lastMention(@NotNull Block block, SimpleName variable, int startAt) {
    return lastMention(block, variable, startAt, block.statements().size());
  }

  protected StatementOfInterest lastMention(@NotNull Block block, SimpleName variable, int startAt, int endAt) {
    IBinding binding = variable.resolveBinding();
    if (null == binding) {
      warn(variable, "Unresolved binding");
      return null;
    } else {
      final List statements = block.statements();
      StatementOfInterest lastMention1 = null;
      for (int j = startAt; j < endAt; j++) {
        final Statement statement = (Statement) statements.get(j);
        if (contains(statement, binding)) {
          lastMention1 = new StatementOfInterest(statement, j);
        }
      }
      final StatementOfInterest lastMention = lastMention1;
      if (null == lastMention) {
        info(variable, "No mentions of %s", variable);
      } else {
        info(variable, "Last mentions of %s: %s at line %s\"",
            variable,
            lastMention.statement.toString().trim(),
            lastMention.line);
      }
      return lastMention;
    }
  }

  @NotNull
  protected ExpressionStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression, @NotNull Type simpleType) {
    AST ast = expression.getAST();
    final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
    variableDeclarationFragment.setName(ast.newSimpleName(identifier));
    final VariableDeclarationExpression variableDeclarationExpression = ast.newVariableDeclarationExpression(variableDeclarationFragment);
    variableDeclarationExpression.setType(simpleType);
    final Assignment assignment = ast.newAssignment();
    assignment.setLeftHandSide(variableDeclarationExpression);
    assignment.setOperator(Assignment.Operator.ASSIGN);
    assignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, expression));
    return ast.newExpressionStatement(assignment);
  }

  protected ExpressionStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression) {
    final Type type = getType(expression, true);
    if (null == type) {
      warn(expression, "Cannot resolve type of %s", expression);
      return null;
    }
    return newLocalVariable(identifier, expression, type);
  }

  protected String randomIdentifier(ASTNode node) {
    final String id = "temp" + Long.toString(Math.abs(random.nextLong())).substring(0, 4);
    info(1, node, "Creating %s", id);
    return id;
  }

  protected void removeMethods(@NotNull AnonymousClassDeclaration node, String methodName) {
    for (final Iterator iterator = node.bodyDeclarations().iterator(); iterator.hasNext(); ) {
      final ASTNode bodyDecl = (ASTNode) iterator.next();
      if (bodyDecl instanceof MethodDeclaration) {
        final SimpleName name = ((MethodDeclaration) bodyDecl).getName();
        if (name.toString().equals(methodName)) {
          iterator.remove();
          info(bodyDecl, "Removing %s", bodyDecl);
        }
      }
    }
  }

  protected void removeMethods(@NotNull TypeDeclaration node, String methodName) {
    for (final Iterator iterator = node.bodyDeclarations().iterator(); iterator.hasNext(); ) {
      final ASTNode bodyDecl = (ASTNode) iterator.next();
      if (bodyDecl instanceof MethodDeclaration) {
        final SimpleName name = ((MethodDeclaration) bodyDecl).getName();
        if (name.toString().equals(methodName)) {
          iterator.remove();
          info(bodyDecl, "Removing %s", bodyDecl);
        }
      }
    }
  }

  protected void replace(ASTNode child, ASTNode newChild) {
    final ASTNode parent = child.getParent();
    if (parent instanceof QualifiedName) {
      final QualifiedName qualifiedName = (QualifiedName) parent;
      if (qualifiedName.getQualifier().equals(child)) {
        if (!(newChild instanceof Name) && (newChild instanceof Expression)) {
          final AST ast = child.getAST();
          final FieldAccess fieldAccess = ast.newFieldAccess();
          fieldAccess.setExpression((Expression) ASTNode.copySubtree(ast, newChild));
          fieldAccess.setName((SimpleName) ASTNode.copySubtree(ast, qualifiedName.getName()));
          info(child, "Replacing %s with %s", child, newChild);
          replace(parent, fieldAccess);
          return;
        }
      }
    }
    StructuralPropertyDescriptor location = child.getLocationInParent();
    if (location != null) {
      if (location.isChildProperty()) {
        info(1, child, "Replace %s with %s within %s (%s)", child, newChild, parent, parent.getClass().getSimpleName());
        parent.setStructuralProperty(location, newChild);
      } else {
        if (location.isChildListProperty()) {
          info(1, child, "Replace %s with %s", child, newChild);
          List l = (List) parent.getStructuralProperty(location);
          final int indexOf = l.indexOf(child);
          l.set(indexOf, newChild);
        } else {
          warn(1, child, "Failed to replace %s with %s", child, newChild);
        }
      }
    } else {
      warn(1, child, "Failed to replace %s with %s", child, newChild);
    }
  }

  @NotNull
  protected String toString(StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  protected void warn(ASTNode node, String formatString, Object... args) {
    warn(1, node, formatString, args);
  }

  protected void warn(int frames, ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.warn(String.format(getFormatString(node, formatString, caller), args));
  }

}
