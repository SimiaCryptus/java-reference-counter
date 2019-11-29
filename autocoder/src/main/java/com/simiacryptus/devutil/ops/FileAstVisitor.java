package com.simiacryptus.devutil.ops;

import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class FileAstVisitor extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(FileAstVisitor.class);
  @NotNull
  private static final Random random = new Random();
  protected final CompilationUnit compilationUnit;
  protected final File file;

  protected FileAstVisitor(CompilationUnit compilationUnit, File file) {
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
    final String binaryName = typeBinding.getBinaryName();
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
  protected static ArrayType arrayType(@NotNull AST ast, @NotNull String fqTypeName) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)));
  }

  @NotNull
  protected static MarkerAnnotation annotation_override(@NotNull AST ast) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName("Override"));
    return annotation;
  }

  @NotNull
  protected static Type getType(@Nonnull AST ast, @NotNull String name) {
    if (name.endsWith("[]")) {
      return ast.newArrayType(getType(ast, name.substring(0, name.length() - 2)));
    } else if (name.contains("\\.")) {
      return ast.newSimpleType(newQualifiedName(ast, name.split("\\.")));
    } else {
      final PrimitiveType.Code typeCode = PrimitiveType.toCode(name);
      if (null != typeCode) {
        return ast.newPrimitiveType(typeCode);
      } else {
        final int typeArg = name.indexOf('<');
        if (typeArg < 0) {
          return ast.newSimpleType(newQualifiedName(ast, name.split("\\.")));
        } else {
          final String mainType = name.substring(0, typeArg);
          final String innerType = name.substring(typeArg + 1, name.length() - 1);
          int nesting = 0;
          ArrayList<Integer> primaryDelimiters = new ArrayList<>();
          for (int i = 0; i < innerType.length(); i++) {
            final char c = innerType.charAt(i);
            if (c == '<') nesting++;
            if (c == '>') nesting--;
            if (c == ',') primaryDelimiters.add(i);
          }
          final ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(newQualifiedName(ast, mainType.split("\\."))));
          if (primaryDelimiters.isEmpty()) {
            parameterizedType.typeArguments().add(getType(ast, innerType));
          } else {
            for (int i = 0; i < primaryDelimiters.size(); i++) {
              final int to = primaryDelimiters.get(i);
              final int from = i == 0 ? 0 : (primaryDelimiters.get(i - 1) + 1);
              parameterizedType.typeArguments().add(getType(ast, innerType.substring(from, to)));
            }
          }
          return parameterizedType;
        }
      }
    }
  }

  protected boolean contains(ASTNode expression, IBinding variableBinding) {
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
    final ASTNode parent = statement.getParent();
    if (parent instanceof Block) {
      final Block block = (Block) parent;
      final ASTNode blockParent = block.getParent();
      if (block.statements().size() == 1) {
        if (blockParent instanceof Statement) {
          delete((Statement) blockParent);
          return;
        }
      }
    } else if (parent instanceof Statement) {
      delete((Statement) parent);
      return;
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

  protected Optional<MethodDeclaration> findMethod(@NotNull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  protected Optional<MethodDeclaration> findMethod(@NotNull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x -> x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration) methodDeclaration).getName().toString().equals(name)).findFirst();
  }

  protected String getFormatString(ASTNode node, String formatString, StackTraceElement caller) {
    return String.format("(%s) (%s) - %s", toString(caller), getLocation(node), formatString);
  }

  @NotNull
  protected String getLocation(ASTNode node) {
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

  @NotNull
  protected Type getType(@NotNull Expression node) {
    final ITypeBinding typeBinding = node.resolveTypeBinding();
    if (null == typeBinding) {
      warn(1, node, "Unresolved binding");
      return null;
    }
    return getType(node.getAST(), typeBinding.getName());
  }

  protected void info(ASTNode node, String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  protected void info(int frames, ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getFormatString(node, formatString, caller), Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  @NotNull
  protected List<IndexSymbols.Mention> lastMentions(@NotNull Block block, IBinding variable) {
    final List statements = block.statements();
    final ArrayList<IndexSymbols.Mention> mentions = new ArrayList<>();
    IndexSymbols.Mention lastMention = null;
    for (int j = 0; j < statements.size(); j++) {
      final Statement statement = (Statement) statements.get(j);
      if (statement instanceof IfStatement) {
        final IfStatement ifStatement = (IfStatement) statement;
        final Statement thenStatement = ifStatement.getThenStatement();
        if (thenStatement instanceof Block) {
          mentions.addAll(lastMentions((Block) thenStatement, variable)
              .stream().filter(x -> x.isReturn()).collect(Collectors.toList()));
        } else if (thenStatement instanceof ReturnStatement && contains(thenStatement, variable)) {
          new IndexSymbols.Mention(block, j, thenStatement);
        }
        final Statement elseStatement = ifStatement.getElseStatement();
        if (elseStatement instanceof Block) {
          mentions.addAll(lastMentions((Block) elseStatement, variable)
              .stream().filter(x -> x.isReturn()).collect(Collectors.toList()));
        } else if (elseStatement instanceof ReturnStatement && contains(elseStatement, variable)) {
          new IndexSymbols.Mention(block, j, elseStatement);
        }
        if (contains(ifStatement.getExpression(), variable)) {
          lastMention = new IndexSymbols.Mention(block, j, ifStatement);
        }
      } else if (contains(statement, variable)) {
        lastMention = new IndexSymbols.Mention(block, j, statement);
      }
    }
    if (null != lastMention) mentions.add(lastMention);
    return mentions;
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

  @NotNull
  protected ExpressionStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression) {
    return newLocalVariable(identifier, expression, getType(expression));
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
          info(child, "Replace %s with %s", child, newChild);
          List l = (List) parent.getStructuralProperty(location);
          final int indexOf = l.indexOf(child);
          l.set(indexOf, newChild);
        } else {
          warn(child, "Failed to replace %s with %s", child, newChild);
        }
      }
    } else {
      warn(child, "Failed to replace %s with %s", child, newChild);
    }
  }

  @NotNull
  protected String toString(StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  @NotNull
  protected String toString(IPackageBinding declaringClassPackage) {
    return Arrays.stream(declaringClassPackage.getNameComponents()).reduce((a, b) -> a + "." + b).get();
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
