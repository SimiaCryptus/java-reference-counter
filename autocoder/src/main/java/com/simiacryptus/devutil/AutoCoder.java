package com.simiacryptus.devutil;

import com.simiacryptus.devutil.ops.IndexSymbols;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class AutoCoder extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(AutoCoder.class);
  @NotNull
  protected final SimpleMavenProject project;

  public AutoCoder(@NotNull String pathname) {
    try {
      this.project = SimpleMavenProject.load(new File(pathname).getCanonicalPath());
    } catch (@NotNull IOException | PlexusContainerException | DependencyResolutionException | ProjectBuildingException | ComponentLookupException e) {
      throw new RuntimeException(e);
    }

  }

  @NotNull
  public static String toString(IPackageBinding declaringClassPackage) {
    return Arrays.stream(declaringClassPackage.getNameComponents()).reduce((a, b) -> a + "." + b).get();
  }

  public static <T> T getField(Object obj, String name) {
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

  public static void replace(ASTNode child, ASTNode newChild) {
    StructuralPropertyDescriptor location = child.getLocationInParent();
    if (location == null) {
      return;
    }
    if (location.isChildProperty()) {
      child.getParent().setStructuralProperty(location, newChild);
      return;
    }
    if (location.isChildListProperty()) {
      List l = (List) child.getParent().getStructuralProperty(location);
      final int indexOf = l.indexOf(child);
      l.set(indexOf, newChild);
    }
  }

  @Nonnull
  public abstract void rewrite();

  public static List<IMethodBinding> enclosingMethods(ASTNode node) {
    final ArrayList<IMethodBinding> list = new ArrayList<>();
    if(null != node) {
      if(node instanceof MethodDeclaration) {
        list.addAll(enclosingMethods(node.getParent()));
        list.add(((MethodDeclaration) node).resolveBinding());
      } else if(null != node.getParent()) {
        list.addAll(enclosingMethods(node.getParent()));
      }
    }
    return list;
  }

  public int rewrite(@NotNull BiFunction<CompilationUnit, File, ASTVisitor> visitor) {
    return project.parse().entrySet().stream().mapToInt(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      final String prevSrc = compilationUnit.toString();
      final ASTVisitor astVisitor = visitor.apply(compilationUnit, file);
      compilationUnit.accept(astVisitor);
      final String finalSrc = compilationUnit.toString();
      if (!prevSrc.equals(finalSrc)) {
        logger.info(String.format("Changed: %s with %s", file, astVisitor.getClass().getSimpleName()));
        try {
          FileUtils.write(file, format(finalSrc), "UTF-8");
          return 1;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        logger.debug("Not Touched: " + file);
        return 0;
      }
    }).sum();
  }

  public void scan(@NotNull BiFunction<CompilationUnit, File, ASTVisitor> visitor) {
    project.parse().entrySet().stream().forEach(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      compilationUnit.accept(visitor.apply(compilationUnit, file));
    });
  }

  public String format(@NotNull String finalSrc) {
    final Document document = new Document();
    document.set(finalSrc);
    try {
      new DefaultCodeFormatter(formattingSettings())
          .format(
              CodeFormatter.K_COMPILATION_UNIT,
              finalSrc,
              0,
              finalSrc.length(),
              0,
              "\n")
          .apply(document);
    } catch (BadLocationException e) {
      throw new RuntimeException();
    }
    return document.get();
  }

  protected static void removeMethods(@NotNull AnonymousClassDeclaration node, String methodName) {
    for (final Iterator iterator = node.bodyDeclarations().iterator(); iterator.hasNext(); ) {
      final Object next = iterator.next();
      if (next instanceof MethodDeclaration) {
        final SimpleName name = ((MethodDeclaration) next).getName();
        if (name.toString().equals(methodName)) {
          iterator.remove();
        }
      }
    }
  }

  protected static void removeMethods(@NotNull TypeDeclaration node, String methodName) {
    for (final Iterator iterator = node.bodyDeclarations().iterator(); iterator.hasNext(); ) {
      final Object next = iterator.next();
      if (next instanceof MethodDeclaration) {
        final SimpleName name = ((MethodDeclaration) next).getName();
        if (name.toString().equals(methodName)) {
          iterator.remove();
        }
      }
    }
  }

  @NotNull
  public <T> T setField(@NotNull T astNode, String name, Object value) {
    try {
      getField(astNode.getClass(), name).set(astNode, value);
      return astNode;
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public Field getField(@NotNull Class<?> nodeClass, String name) {
    final Field[] fields = nodeClass.getDeclaredFields();
    final Optional<Field> parent = Arrays.stream(fields).filter(x -> x.getName().equals(name)).findFirst();
    if (!parent.isPresent()) {
      final Class<?> superclass = nodeClass.getSuperclass();
      if (superclass != null) {
        return getField(superclass, name);
      } else {
        throw new AssertionError(String.format("Cannot find field %s", name));
      }
    }
    final Field field = parent.get();
    field.setAccessible(true);
    return field;
  }

  @NotNull
  protected DefaultCodeFormatterOptions formattingSettings() {
    final DefaultCodeFormatterOptions javaConventionsSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
    javaConventionsSettings.align_with_spaces = true;
    javaConventionsSettings.tab_char = DefaultCodeFormatterOptions.SPACE;
    javaConventionsSettings.indentation_size = 2;
    return javaConventionsSettings;
  }

  protected static boolean derives(@Nonnull ITypeBinding typeBinding, @Nonnull Class<?> baseClass) {
    final String binaryName = typeBinding.getBinaryName();
    if (null != binaryName && binaryName.equals(baseClass.getCanonicalName())) return true;
    if (typeBinding.getSuperclass() != null) return derives(typeBinding.getSuperclass(), baseClass);
    if (typeBinding.isArray()) return derives(typeBinding.getElementType(), baseClass);
    return false;
  }


  @NotNull
  public static Name newQualifiedName(AST ast, Class<?> clazz) {
    return newQualifiedName(ast, clazz.getName().split("\\."));
  }

  @NotNull
  public static Name newQualifiedName(@NotNull AST ast, @NotNull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(i -> new String[i])), simpleName);
  }

  @NotNull
  public static ArrayType arrayType(@NotNull AST ast, @NotNull String fqTypeName) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)));
  }

  @NotNull
  public static MarkerAnnotation annotation_override(@NotNull AST ast) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName("Override"));
    return annotation;
  }

  @NotNull
  public static ExpressionStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression) {
    return newLocalVariable(identifier, expression, getType(expression));
  }

  @NotNull
  public static ExpressionStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression, @NotNull Type simpleType) {
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
  public static Type getType(@NotNull Expression expression) {
    return getType(expression.getAST(), expression.resolveTypeBinding().getName());
  }

  public static Optional<MethodDeclaration> findMethod(@NotNull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  public static Optional<MethodDeclaration> findMethod(@NotNull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x->x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration)methodDeclaration).getName().toString().equals(name)).findFirst();
  }

  @NotNull
  public IndexSymbols.SymbolIndex getSymbolIndex() {
    final IndexSymbols.SymbolIndex index = new IndexSymbols.SymbolIndex();
    scan((cu, file) -> new IndexSymbols(cu, file, index));
    return index;
  }

  @NotNull
  public static List<IndexSymbols.Mention> lastMentions(@NotNull Block block, IBinding variable) {
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

  private static boolean contains(ASTNode expression, IBinding variableBinding) {
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

  @NotNull
  public static Type getType(@Nonnull AST ast, @NotNull String name) {
    if (name.endsWith("[]")) {
      return ast.newArrayType(getType(ast, name.substring(0, name.length() - 2)));
    } else if (name.contains("\\.")) {
      return ast.newSimpleType(newQualifiedName(ast, name.split("\\.")));
    } else {
      final PrimitiveType.Code typeCode = PrimitiveType.toCode(name);
      if (null != typeCode) {
        return ast.newPrimitiveType(typeCode);
      } else {
        return ast.newSimpleType(ast.newSimpleName(name));
      }
    }
  }

  public static void delete(@NotNull Statement parent) {
    final ASTNode parent1 = parent.getParent();
    if (parent1 instanceof Block) {
      final Block block = (Block) parent1;
      if (block.statements().size() == 1) {
        final ASTNode blockParent = block.getParent();
        if (blockParent instanceof Statement) {
          delete(parent);
          return;
        }
      }
    } else if (parent1 instanceof Statement) {
      delete((Statement) parent1);
      return;
    }
    parent.delete();
  }

}
