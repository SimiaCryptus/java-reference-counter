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

import com.simiacryptus.ref.core.AutoCoder;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.text.edits.TextEdit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RefIgnore
public abstract class FileAstVisitor extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(FileAstVisitor.class);
  @NotNull
  private static final Random random = new Random();
  protected final CompilationUnit compilationUnit;
  protected final File file;
  protected final ProjectInfo projectInfo;
  protected final String initialContent;
  private CompilationUnit reparsed = null;
  ;

  public FileAstVisitor(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this.projectInfo = projectInfo;
    this.compilationUnit = compilationUnit;
    this.file = file;
    this.initialContent = AutoCoder.read(this.file);
  }

  protected final static <T> T getField(Object obj, String name) {
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

  protected final static boolean derives(@Nonnull ITypeBinding typeBinding, @Nonnull Class<?> baseClass) {
    final String binaryName = typeBinding.getTypeDeclaration().getQualifiedName();
    if (null != binaryName && binaryName.equals(baseClass.getCanonicalName())) return true;
    if (Arrays.stream(typeBinding.getInterfaces()).filter(x -> derives(x, baseClass)).findAny().isPresent()) return true;
    if (typeBinding.getSuperclass() != null) return derives(typeBinding.getSuperclass(), baseClass);
    if (typeBinding.isArray()) return derives(typeBinding.getElementType(), baseClass);
    return false;
  }

  @NotNull
  protected final static Name newQualifiedName(AST ast, Class<?> clazz) {
    return newQualifiedName(ast, clazz.getName().split("\\."));
  }

  @NotNull
  protected final static Name newQualifiedName(@NotNull AST ast, @NotNull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(String[]::new)), simpleName);
  }

  @NotNull
  protected final static ArrayType arrayType(@NotNull AST ast, @NotNull String fqTypeName, int rank) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)), rank);
  }

  @NotNull
  protected final static MarkerAnnotation annotation_override(@NotNull AST ast) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName("Override"));
    return annotation;
  }

  protected final static boolean hasAnnotation(IBinding declaringClass, Class<?> aClass) {
    return Arrays.stream(declaringClass.getAnnotations())
        .map(annotation -> annotation.getAnnotationType().getQualifiedName())
        .anyMatch(qualifiedName -> qualifiedName.equals(aClass.getCanonicalName()));
  }

  protected final static ITypeBinding resolveBinding(TypeDeclaration typeDeclaration) {
    return typeDeclaration.resolveBinding();
  }

  @NotNull
  protected final Stream<IMethodBinding> allMethods(ITypeBinding targetClass) {
    final ITypeBinding superclass = targetClass.getSuperclass();
    Stream<IMethodBinding> declaredMethods = Arrays.stream(targetClass.getDeclaredMethods());
    if (null != superclass && !superclass.getQualifiedName().equals(Object.class.getCanonicalName())) {
      declaredMethods = Stream.concat(declaredMethods, allMethods(superclass));
    }
    declaredMethods = Stream.concat(declaredMethods, Arrays.stream(targetClass.getInterfaces()).flatMap(this::allMethods));
    return declaredMethods.distinct();
  }

  protected final boolean contains(@Nonnull ASTNode expression, @Nonnull IBinding variableBinding) {
    final AtomicBoolean found = new AtomicBoolean(false);
    expression.accept(new ASTVisitor() {
      @Override
      public void endVisit(@NotNull SimpleName node) {
        final IBinding binding = resolveBinding(node);
        if (null != binding && binding.equals(variableBinding)) found.set(true);
      }
    });
    return found.get();
  }

  @Nullable
  protected final ASTNode copySubtree(AST ast, ASTNode node) {
    return ASTNode.copySubtree(ast, node);
  }

  protected final void debug(ASTNode node, String formatString, Object... args) {
    debug(1, node, formatString, args);
  }

  protected final void debug(int frames, ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.debug(String.format(getFormatString(node, formatString, caller), args));
  }

  protected final void delete(@NotNull Statement statement) {
    info(1, statement, "Deleting %s", statement);
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
            replace(tryStatement, copySubtree(ast, tryStatement.getBody()));
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
          ifStatement.setThenStatement((Statement) copySubtree(ast, elseStatement));
          final PrefixExpression prefixExpression = ast.newPrefixExpression();
          prefixExpression.setOperand((Expression) copySubtree(ast, condition));
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

  protected final List<IMethodBinding> enclosingMethods(ASTNode node) {
    final ArrayList<IMethodBinding> list = new ArrayList<>();
    if (null != node) {
      if (node instanceof MethodDeclaration) {
        list.addAll(enclosingMethods(node.getParent()));
        final MethodDeclaration methodDeclaration = (MethodDeclaration) node;
        final IMethodBinding methodBinding = resolveBinding(methodDeclaration);
        if (null != methodBinding) list.add(methodBinding);
      } else if (null != node.getParent()) {
        list.addAll(enclosingMethods(node.getParent()));
      }
    }
    return list;
  }

  protected final List<StatementOfInterest> exits(@NotNull Block block, int startAt, int endAt) {
    final ArrayList<StatementOfInterest> exits = exits_(block, startAt, endAt);
    info(1, block, "Exits from %s to %s: \n\t%s", startAt, endAt, exits.stream().map(x ->
        String.format("%s at line %s", x.statement.toString().trim(), x.line)
    ).reduce((a, b) -> a + "\n\t" + b).orElse(""));
    return exits;
  }

  @NotNull
  private ArrayList<StatementOfInterest> exits_(@NotNull Block block, int startAt, int endAt) {
    final List statements = block.statements();
    final ArrayList<StatementOfInterest> exits = new ArrayList<>();
    final AST ast = block.getAST();
    for (int j = Math.max(0, startAt); j <= endAt; j++) {
      final Statement statement = (Statement) statements.get(j);
      if (statement instanceof IfStatement) {
        final IfStatement ifStatement = (IfStatement) statement;
        exits.addAll(exits_(ast, ifStatement::getThenStatement, ifStatement::setThenStatement));
        exits.addAll(exits_(ast, ifStatement::getElseStatement, ifStatement::setElseStatement));
      } else if (statement instanceof WhileStatement) {
        final WhileStatement whileStatement = (WhileStatement) statement;
        exits.addAll(exits_(ast, whileStatement::getBody, whileStatement::setBody));
      } else if (statement instanceof DoStatement) {
        final DoStatement doStatement = (DoStatement) statement;
        exits.addAll(exits_(ast, doStatement::getBody, doStatement::setBody));
      } else if (statement instanceof ForStatement) {
        final ForStatement forStatement = (ForStatement) statement;
        exits.addAll(exits_(ast, forStatement::getBody, forStatement::setBody));
      } else if (statement instanceof EnhancedForStatement) {
        final EnhancedForStatement forStatement = (EnhancedForStatement) statement;
        exits.addAll(exits_(ast, forStatement::getBody, forStatement::setBody));
      } else if (statement instanceof TryStatement) {
        final TryStatement tryStatement = (TryStatement) statement;
        exits.addAll(exits_(ast, tryStatement::getBody, tryStatement::setBody));
      } else if (statement instanceof SynchronizedStatement) {
        final SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
        exits.addAll(exits_(ast, synchronizedStatement::getBody, synchronizedStatement::setBody));
      } else if (isExit(statement)) {
        exits.add(new StatementOfInterest(statement, j));
      }
    }
    return exits;
  }

  private List<StatementOfInterest> exits_(AST ast, Supplier<Statement> supplier, Consumer<Block> consumer) {
    Statement body = supplier.get();
    if (!(body instanceof Block) && isExit(body)) {
      final Block newBlock = ast.newBlock();
      newBlock.statements().add(copySubtree(ast, body));
      consumer.accept(newBlock);
      body = newBlock;
    }
    if (body instanceof Block) {
      final Block thenBlock = (Block) body;
      return exits_(thenBlock, 0, (thenBlock).statements().size() - 1);
    } else {
      return new ArrayList<>();
    }
  }

  protected final <T extends ASTNode> T findExpression(ASTNode tree, T searchFor) {
    return findExpressions(tree, searchFor).stream().findAny().orElse(null);
  }

  protected final <T extends ASTNode> List<T> findExpressions(ASTNode tree, T searchFor) {
    final List<T> reference = new ArrayList<>();
    final Class<T> searchForClass = (Class<T>) searchFor.getClass();
    final String searchForString = searchFor.toString();
    tree.accept(new ASTVisitor() {
      @Override
      public void postVisit(ASTNode node) {
        if (node.getClass().equals(searchForClass)) {
          if (node.toString().equals(searchForString)) {
            reference.add((T) node);
          }
        }
      }
    });
    return reference;
  }

  protected final Optional<MethodDeclaration> findMethod(@NotNull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  protected final Optional<MethodDeclaration> findMethod(@NotNull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x -> x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration) methodDeclaration).getName().toString().equals(name)).findFirst();
  }

  private <T extends ASTNode> Optional<T> findReparsed(@NotNull T node) {
    if (compilationUnit.getAST().modificationCount() <= 0) return Optional.empty();
    if (null != reparsed) {
      final Optional<T> any = findExpressions(reparsed, node).stream().filter(x -> reparseMatch(node, x)).findAny();
      if (any.isPresent()) return any;
    }
    reparsed = reparse();
    final List<T> reparsedMatches = findExpressions(reparsed, node);
    final Optional<T> any = reparsedMatches.stream().filter(x -> reparseMatch(node, x)).findAny();
    if (!any.isPresent()) {
      warn(node, "Could not find %s at %s in reparsed tree; %s candidates", node, node.getStartPosition(), reparsedMatches.size());
    }
    return any;
  }

  @Nullable
  protected final Block getBlock(ASTNode node) {
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

  protected final List<Statement> getEvaluableStatements(Expression expression) {
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
        statements.add(ast.newExpressionStatement((Expression) copySubtree(ast, expression)));
      }
    }
    return statements;
  }

  protected final String getFormatString(@Nonnull ASTNode node, @Nonnull String formatString, @Nonnull StackTraceElement caller) {
    return String.format("(%s) (%s) - %s", toString(caller), getLocation(node), formatString);
  }

  protected final LambdaExpression getLambda(ASTNode node) {
    if (node == null) return null;
    if (node instanceof LambdaExpression) return (LambdaExpression) node;
    return getLambda(node.getParent());
  }

  protected final int getLineNumber(Block block, ASTNode node) {
    return block.statements().indexOf(getStatement(node));
  }

  @NotNull
  protected final String getLocation(@Nonnull ASTNode node) {
    final int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
    return file.getName() + ":" + lineNumber;
  }

  @NotNull
  protected final IndexSymbols.Span getSpan(ASTNode node) {
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

  protected final Statement getStatement(ASTNode node) {
    if (node == null) return null;
    if (node instanceof Statement) return (Statement) node;
    return getStatement(node.getParent());
  }

  @NotNull
  protected final IndexSymbols.SymbolIndex getSymbolIndex(ASTNode node) {
    final IndexSymbols.SymbolIndex lambdaIndex = new IndexSymbols.SymbolIndex();
    node.accept(new IndexSymbols(projectInfo, compilationUnit, file, lambdaIndex) {
      @Override
      public void endVisit(QualifiedName node) {
        Name root = node;
        while (root instanceof QualifiedName) {
          root = ((QualifiedName) root).getQualifier();
        }
        final IBinding binding = resolveBinding(root);
        if (null != binding) {
          indexReference(root, binding);
        }
      }
    }.setVerbose(false));
    return lambdaIndex;
  }

  protected final String getTempIdentifier(ASTNode node) {
    final String id = "temp" + Long.toString(Math.abs(random.nextLong())).substring(0, 4);
    info(1, node, "Creating %s", id);
    return id;
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

  protected final Type getType(ASTNode node, @NotNull String name, boolean isDeclaration) {
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

  protected final Type getType(@NotNull Expression node, boolean isDeclaration) {
    final ITypeBinding typeBinding = resolveTypeBinding(node);
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

  protected final boolean hasReturnValue(LambdaExpression lambdaExpression) {
    ASTNode parent = lambdaExpression.getParent();
    if (!(parent instanceof MethodInvocation)) {
      info(lambdaExpression, "lambda %s has no return value", lambdaExpression);
      return false;
    }
    return hasReturnValue((MethodInvocation) parent, lambdaExpression);
  }

  @Nullable
  protected final boolean hasReturnValue(MethodInvocation methodInvocation, LambdaExpression lambdaExpression) {
    final int argIndex = methodInvocation.arguments().indexOf(lambdaExpression);
    final ITypeBinding targetClass = resolveMethodBinding(methodInvocation).getParameterTypes()[argIndex];
    if (derives(targetClass, Consumer.class)) {
      info(methodInvocation, "lambda %s has no return value", lambdaExpression);
      return false;
    } else if (derives(targetClass, Function.class)) {
      info(methodInvocation, "lambda %s has return value", lambdaExpression);
      return true;
    } else if (derives(targetClass, Predicate.class)) {
      info(methodInvocation, "lambda %s has return value", lambdaExpression);
      return true;
    } else {
      final List<IMethodBinding> methods = allMethods(targetClass)
          .filter(x -> (x.getModifiers() & Modifier.STATIC) == 0)
          .filter(x -> (x.getModifiers() & Modifier.DEFAULT) == 0)
          .collect(Collectors.toList());
      if (methods.size() == 1) {
        final ITypeBinding returnType = methods.get(0).getReturnType();
        if (returnType.equals(PrimitiveType.VOID)) {
          info(methodInvocation, "lambda %s has no return value", lambdaExpression);
          return false;
        } else {
          info(methodInvocation, "Lambda interface %s returns a %s value", targetClass.getQualifiedName(), returnType.getQualifiedName());
          return true;
        }
      } else {
        warn(methodInvocation, "Cannot determine if %s returns a value; it has %s methods", targetClass.getQualifiedName(), methods.size());
        return false;
      }
    }
  }

  protected final void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  protected final void info(int frames, @Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getFormatString(node, formatString, caller), Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  protected final boolean isEvaluable(Expression node) {
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

  protected final boolean isExit(Statement statement) {
    if (statement instanceof ReturnStatement) return true;
    if (statement instanceof ThrowStatement) return true;
    return false;
  }

  protected final boolean isField(SimpleName simpleName) {
    final IBinding iBinding = simpleName.resolveBinding();
    final boolean isVariable = iBinding instanceof IVariableBinding;
    boolean isField = false;
    if (isVariable) {
      isField = getField(iBinding, "binding") instanceof FieldBinding;
    }
    return isField;
  }

  protected final boolean isTempIdentifier(SimpleName name) {
    return Pattern.matches("temp\\d{0,4}", name.toString());
  }

  protected final StatementOfInterest lastMention(@NotNull Block block, SimpleName variable, int startAt) {
    return lastMention(block, variable, startAt, block.statements().size(), 2);
  }

  protected final StatementOfInterest lastMention(@NotNull Block block, SimpleName variable, int startAt, int endAt, int frames) {
    IBinding binding = resolveBinding(variable);
    if (null == binding) {
      warn(frames, variable, "Unresolved binding");
      return null;
    } else {
      final List statements = block.statements();
      StatementOfInterest lastMention = null;
      for (int j = Math.max(startAt, 0); j < endAt; j++) {
        final Statement statement = (Statement) statements.get(j);
        if (!findExpressions(statement, variable).isEmpty()) {
          lastMention = new StatementOfInterest(statement, j);
        }
      }
      if (null == lastMention) {
        info(frames, variable, "No mention of %s", variable);
      } else {
        info(frames, variable, "Last mention of %s: %s at line %s\"",
            variable,
            lastMention.statement.toString().trim(),
            lastMention.line);
      }
      return lastMention;
    }
  }

  protected final VariableDeclarationStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression, @NotNull Type type) {
    AST ast = expression.getAST();
    final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
    variableDeclarationFragment.setName(ast.newSimpleName(identifier));
    variableDeclarationFragment.setInitializer((Expression) copySubtree(ast, expression));
    final VariableDeclarationStatement variableDeclarationStatement = ast.newVariableDeclarationStatement(variableDeclarationFragment);
    variableDeclarationStatement.setType(type);
    return variableDeclarationStatement;
  }

  protected final VariableDeclarationStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression) {
    final Type type = getType(expression, true);
    if (null == type) {
      warn(expression, "Cannot resolve type of %s", expression);
      return null;
    }
    return newLocalVariable(identifier, expression, type);
  }

  protected final void removeMethods(@NotNull AnonymousClassDeclaration node, String methodName) {
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

  protected final void removeMethods(@NotNull TypeDeclaration node, String methodName) {
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

  private CompilationUnit reparse() {
    logger.info(String.format("Writing intermediate changes to %s", file));
    try {
      write(false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return projectInfo.read(file).values().iterator().next();
  }

  private <T extends ASTNode> boolean reparseMatch(@NotNull T a, @NotNull T b) {
    if (!strEquals(a, b)) {
      return false;
    }
    final ASTNode parentA = a.getParent();
    final ASTNode parentB = b.getParent();
    if (parentA == null && parentB == null) return true;
    if (parentA == null || parentB == null) {
      return false;
    }
    return reparseMatch(parentA, parentB);
  }

  protected final void replace(ASTNode child, ASTNode newChild) {
    final ASTNode parent = child.getParent();
    if (parent instanceof QualifiedName) {
      final QualifiedName qualifiedName = (QualifiedName) parent;
      if (qualifiedName.getQualifier().equals(child)) {
        if (!(newChild instanceof Name) && (newChild instanceof Expression)) {
          final AST ast = child.getAST();
          final FieldAccess fieldAccess = ast.newFieldAccess();
          fieldAccess.setExpression((Expression) copySubtree(ast, newChild));
          fieldAccess.setName((SimpleName) copySubtree(ast, qualifiedName.getName()));
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

  @Nullable
  protected final <T extends ASTNode, R extends IBinding> R resolve(T node, Function<T, R> function) {
    return Optional.of(node).map(function).filter(Objects::nonNull).orElseGet(() ->
        findReparsed(node).map(function).orElseGet(() -> null));
  }

  protected final IBinding resolveBinding(@NotNull Name node) {
    return resolve(node, Name::resolveBinding);
  }

  protected final IMethodBinding resolveBinding(MethodDeclaration node) {
    return resolve(node, MethodDeclaration::resolveBinding);
  }

  protected final IVariableBinding resolveBinding(VariableDeclarationFragment node) {
    return resolve(node, VariableDeclaration::resolveBinding);
  }

  protected final IVariableBinding resolveBinding(SingleVariableDeclaration node) {
    return resolve(node, VariableDeclaration::resolveBinding);
  }

  protected final ITypeBinding resolveBinding(TypeParameter node) {
    return resolve(node, TypeParameter::resolveBinding);
  }

  protected final ITypeBinding resolveBinding(Type node) {
    return resolve(node, Type::resolveBinding);
  }

  protected final ITypeBinding resolveBinding(AnonymousClassDeclaration node) {
    return resolve(node, AnonymousClassDeclaration::resolveBinding);
  }

  protected final IVariableBinding resolveBinding(@NotNull VariableDeclaration node) {
    return resolve(node, VariableDeclaration::resolveBinding);
  }

  protected final IMethodBinding resolveConstructorBinding(@NotNull ClassInstanceCreation node) {
    return resolve(node, ClassInstanceCreation::resolveConstructorBinding);
  }

  protected final IMethodBinding resolveConstructorBinding(@NotNull ConstructorInvocation node) {
    return resolve(node, ConstructorInvocation::resolveConstructorBinding);
  }

  protected final IVariableBinding resolveFieldBinding(FieldAccess node) {
    return resolve(node, FieldAccess::resolveFieldBinding);
  }

  protected final IMethodBinding resolveMethodBinding(LambdaExpression node) {
    return resolve(node, LambdaExpression::resolveMethodBinding);
  }

  protected final IMethodBinding resolveMethodBinding(MethodInvocation node) {
    return resolve(node, MethodInvocation::resolveMethodBinding);
  }

  protected final ITypeBinding resolveTypeBinding(@NotNull Expression node) {
    return resolve(node, Expression::resolveTypeBinding);
  }

  public boolean revert() {
    final String currentContent = AutoCoder.read(this.file);
    if (currentContent.equals(initialContent)) return false;
    try {
      FileUtils.write(file, initialContent, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  private <T extends ASTNode> boolean strEquals(@NotNull T a, @NotNull T b) {
    return b.toString().replaceAll("\\s+", " ").equals(a.toString().replaceAll("\\s+", " "));
  }

  @NotNull
  protected final Block toBlock(LambdaExpression lambdaExpression) {
    final ASTNode body = lambdaExpression.getBody();
    if (body instanceof Block) {
      return (Block) body;
    } else {
      final AST ast = lambdaExpression.getAST();
      final Block block = ast.newBlock();
      if (hasReturnValue(lambdaExpression)) {
        final ReturnStatement returnStatement = ast.newReturnStatement();
        returnStatement.setExpression((Expression) copySubtree(ast, body));
        block.statements().add(returnStatement);
      } else {
        block.statements().add(ast.newExpressionStatement((Expression) copySubtree(ast, body)));
      }
      info(lambdaExpression, "Replace lambda %s with block %s", lambdaExpression, block);
      lambdaExpression.setBody(block);
      return block;
    }
  }

  @NotNull
  protected final String toString(StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  protected final void warn(ASTNode node, String formatString, Object... args) {
    warn(1, node, formatString, args);
  }

  protected final void warn(int frames, ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.warn(String.format(getFormatString(node, formatString, caller), args));
  }

  public boolean write(boolean format) throws IOException {
    Document document = new Document(initialContent);
    final DocumentRewriteSession rewriteSession = document.startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
    final TextEdit textEdit = compilationUnit.rewrite(document, JavaCore.getOptions());
    try {
      textEdit.apply(document);
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
    document.stopRewriteSession(rewriteSession);
    final String finalSrc = document.get();
    if (initialContent.equals(finalSrc)) return false;
    FileUtils.write(file, format ? AutoCoder.format(finalSrc) : finalSrc, "UTF-8");
    return true;
  }

}
