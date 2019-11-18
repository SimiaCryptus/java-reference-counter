package com.simiacryptus.devutil;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
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
import java.util.function.Function;
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

  @Nonnull
  public abstract void rewrite();

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

  protected void removeMethods(@NotNull TypeDeclaration node, String methodName) {
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

  protected boolean derives(@Nonnull ITypeBinding typeBinding, @Nonnull Class<?> baseClass) {
    if (typeBinding.getBinaryName().equals(baseClass.getCanonicalName())) return true;
    if (typeBinding.getSuperclass() != null) return derives(typeBinding.getSuperclass(), baseClass);
    if (typeBinding.isArray()) return derives(typeBinding.getElementType(), baseClass);
    return false;
  }

  @NotNull
  public static String toString(IPackageBinding declaringClassPackage) {
    return Arrays.stream(declaringClassPackage.getNameComponents()).reduce((a, b) -> a + "." + b).get();
  }

  @NotNull
  public Name newQualifiedName(@NotNull AST ast, @NotNull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(i -> new String[i])), simpleName);
  }

  @NotNull
  public ArrayType arrayType(@NotNull AST ast, @NotNull String fqTypeName) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)));
  }

  @NotNull
  public MarkerAnnotation annotation_override(@NotNull AST ast) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName("Override"));
    return annotation;
  }

  @NotNull
  public ExpressionStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression) {
    return newLocalVariable(identifier, expression, getType(expression));
  }

  @NotNull
  public ExpressionStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression, @NotNull Type simpleType) {
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
  public Type getType(@NotNull Expression expression) {
    return getType(expression.getAST(), expression.resolveTypeBinding().getName());
  }

  public Optional<MethodDeclaration> findMethod(@NotNull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  public static class Mention {
    public final Block block;
    public final int line;
    public final Statement statement;

    public Mention(Block block, int line, Statement statement) {
      this.block = block;
      this.line = line;
      this.statement = statement;
    }

    public boolean isReturn() {
      return statement instanceof ReturnStatement;
    }

    public boolean isComplexReturn() {
      if (!isReturn()) return false;
      return !(((ReturnStatement) statement).getExpression() instanceof Name);
    }

  }

  @NotNull
  public List<Mention> lastMentions(@NotNull Block block, IBinding variable) {
    final List statements = block.statements();
    final ArrayList<Mention> mentions = new ArrayList<>();
    Mention lastMention = null;
    for (int j = 0; j < statements.size(); j++) {
      final Statement statement = (Statement) statements.get(j);
      if (statement instanceof IfStatement) {
        final IfStatement ifStatement = (IfStatement) statement;
        final Statement thenStatement = ifStatement.getThenStatement();
        if (thenStatement instanceof Block) {
          mentions.addAll(lastMentions((Block) thenStatement, variable)
              .stream().filter(x -> x.isReturn()).collect(Collectors.toList()));
        } else if (thenStatement instanceof ReturnStatement && contains(thenStatement, variable)) {
          new Mention(block, j, thenStatement);
        }
        final Statement elseStatement = ifStatement.getElseStatement();
        if (elseStatement instanceof Block) {
          mentions.addAll(lastMentions((Block) elseStatement, variable)
              .stream().filter(x -> x.isReturn()).collect(Collectors.toList()));
        } else if (elseStatement instanceof ReturnStatement && contains(elseStatement, variable)) {
          new Mention(block, j, elseStatement);
        }
        if (contains(ifStatement.getExpression(), variable)) {
          lastMention = new Mention(block, j, ifStatement);
        }
      } else if (contains(statement, variable)) {
        lastMention = new Mention(block, j, statement);
      }
    }
    if (null != lastMention) mentions.add(lastMention);
    return mentions;
  }

  private boolean contains(ASTNode expression, IBinding variableBinding) {
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
  public Type getType(@Nonnull AST ast, @NotNull String name) {
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

  public void delete(@NotNull Statement parent) {
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

  public class FileAstVisitor extends ASTVisitor {
    protected final CompilationUnit compilationUnit;
    protected final File file;

    public FileAstVisitor(CompilationUnit compilationUnit, File file) {
      this.compilationUnit = compilationUnit;
      this.file = file;
    }

    @NotNull
    public String getLocation(ASTNode node) {
      final int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
      return file.getName() + ":" + lineNumber;
    }

    @NotNull
    public Span getSpan(ASTNode node) {
      final int startPosition = node.getStartPosition();
      final int length = node.getLength();

      return new Span(
          file,
          compilationUnit.getLineNumber(startPosition),
          compilationUnit.getColumnNumber(startPosition),
          compilationUnit.getLineNumber(startPosition + length),
          compilationUnit.getColumnNumber(startPosition + length)
      );
    }

  }

  public static class Span {
    public final int lineStart;
    public final int colStart;
    public final int lineEnd;
    private final int colEnd;
    private final File file;

    public Span(File file, int lineStart, int colStart, int lineEnd, int colEnd) {
      this.file = file;
      this.lineStart = lineStart;
      this.colStart = colStart;
      this.lineEnd = lineEnd;
      this.colEnd = colEnd;
    }

    @Override
    public String toString() {
      return String.format("%s:{%d:%d-%d:%d}", file.getName(), lineStart, colStart, lineEnd, colEnd);
    }

    public boolean contains(Span location) {
      if (location.lineStart < lineStart) return false;
      if (location.lineStart == lineStart && location.colStart < colStart) return false;
      if (location.lineEnd > lineEnd) return false;
      if (location.lineEnd == lineEnd && location.colEnd > colEnd) return false;
      return true;
    }
  }

  public static class ContextLocation {
    public final Span location;
    public final LinkedHashMap<BindingId, Span> context;

    public ContextLocation(Span location, LinkedHashMap<BindingId, Span> context) {
      this.location = location;
      this.context = context;
    }
  }

  public static class SymbolIndex {
    public final HashMap<BindingId, ContextLocation> definitionLocations = new HashMap<>();
    public final HashMap<BindingId, ASTNode> definitionNodes = new HashMap<>();
    public final HashMap<BindingId, List<ContextLocation>> references = new HashMap<>();

    public String getPath(IBinding binding) {
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          final FieldBinding fieldBinding = getField(variableBinding, "binding");
          if (fieldBinding != null) {
            final String className = Arrays.stream(fieldBinding.declaringClass.compoundName).map(x -> new String(x)).reduce((a, b) -> a + "." + b).get();
            return String.format("%s::%s",
                className,
                null == variableBinding ? "null" : variableBinding.getName());
          } else {
            return "null::" + variableBinding.getName();
          }
        } else if (variableBinding.isParameter()) {
          final LocalVariableBinding localVariableBinding = getField(variableBinding, "binding");
          final IMethodBinding methodBinding = variableBinding.getDeclaringMethod();
          final String paramName = null == variableBinding ? "?" : new String(localVariableBinding.declaration.name);
          final MethodScope declaringScope = (MethodScope) localVariableBinding.declaringScope;
          if (declaringScope.referenceContext instanceof org.eclipse.jdt.internal.compiler.ast.LambdaExpression) {
            return getPath(((org.eclipse.jdt.internal.compiler.ast.LambdaExpression) declaringScope.referenceContext).binding) + "::" + paramName;
          } else {
            return getPath(methodBinding) + "::" + paramName;
          }
        } else {
          return getPath(variableBinding.getDeclaringMethod()) + "::" + (null == variableBinding ? "null" : variableBinding.getName());
        }
      } else if (binding instanceof IMethodBinding) {
        IMethodBinding methodBinding = getImplementation((IMethodBinding) binding);
        final String typeBinding = null == methodBinding ? null : getPath(methodBinding.getDeclaringClass());
        return typeBinding + "::" + methodName(methodBinding);
      } else if (binding instanceof ITypeBinding) {
        return ((ITypeBinding) binding).getQualifiedName();
      } else if (binding instanceof IPackageBinding) {
        return binding.getName();
      } else {
        throw new RuntimeException(binding.getClass().getCanonicalName());
      }
    }

    public IMethodBinding getImplementation(IMethodBinding methodBinding) {
      while (true) {
        IMethodBinding impl = getField(methodBinding, "implementation");
        if (null != impl && methodBinding != impl) {
          methodBinding = impl;
        } else break;
      }
      return methodBinding;
    }

    @NotNull
    public String getPath(MethodBinding methodBinding) {
      return Arrays.stream(methodBinding.declaringClass.compoundName)
          .map(x -> new String(x)).reduce((a, b) -> a + "." + b).get()
          + "::" + new String(methodBinding.selector);
    }

    public String methodName(IMethodBinding methodBinding) {
      if (null == methodBinding) return "null";
      return String.format("%s(%s)",
          methodBinding.getName(),
          Arrays.stream(methodBinding.getParameterTypes()).map(x -> x.getQualifiedName()).reduce((a, b) -> a + "," + b).orElse("")
      );
    }

    public BindingId describe(@Nonnull IBinding binding) {
      final String path = getPath(binding);
      if(path.contains("::lambda$")) return new BindingId(path, "Lambda");
      else return new BindingId(path, getType(binding));
    }

    public LinkedHashMap<BindingId, Span> context(ASTNode node, Function<ASTNode, Span> locator) {
      final LinkedHashMap<BindingId, Span> list = new LinkedHashMap<>();
      final ASTNode parent = node.getParent();
      if (parent != null) list.putAll(context(parent, locator));
      if (node instanceof MethodDeclaration) {
        list.put(describe(((MethodDeclaration) node).resolveBinding()), locator.apply(node));
      } else if (node instanceof LambdaExpression) {
        final LambdaExpression lambdaExpression = (LambdaExpression) node;
        final IMethodBinding methodBinding = lambdaExpression.resolveMethodBinding();
        list.put(describe(methodBinding).setType("Lambda"), locator.apply(node));
      } else if (node instanceof TypeDeclaration) {
        list.put(describe(((TypeDeclaration) node).resolveBinding()), locator.apply(node));
      }
      return list;
    }

    @NotNull
    public AutoCoder.ContextLocation getContextLocation(ASTNode node, Function<ASTNode, Span> locator) {
      final LinkedHashMap<BindingId, Span> context = context(node, locator);
      return new ContextLocation(locator.apply(node), context);
    }

    public String getType(IBinding binding) {
      String type;
      if (binding instanceof IVariableBinding) {
        final IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          type = "Field";
        } else if (variableBinding.isParameter()) {
          type = "Parameter";
        } else {
          type = "Variable";
        }
      } else if (binding instanceof IMethodBinding) {
        type = "Method";
      } else if (binding instanceof ITypeBinding) {
        type = "Type";
      } else {
        type = String.format("Other (%s)", binding.getClass().getSimpleName());
      }
      return type;
    }
  }

  public class IndexSymbols extends FileAstVisitor {

    SymbolIndex index;
    private boolean verbose = true;

    public IndexSymbols(CompilationUnit compilationUnit, File file, SymbolIndex index) {
      super(compilationUnit, file);
      this.index = index;
    }

    private void indexDef(ASTNode node, IBinding binding) {
      if (null == binding) return;
      final ContextLocation contextLocation = index.getContextLocation(node, this::getSpan);
      final BindingId bindingId = index.describe(binding);
      final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
      if (isVerbose()) logger.info(String.format("Declaration of %s at %s within: \n\t%s", bindingId, getSpan(node), contextPath));
      final ContextLocation replaced = index.definitionLocations.put(bindingId, contextLocation);
      if (null != replaced) throw new RuntimeException(String.format("Duplicate declaration of %s in %s and %s", bindingId, replaced.location, contextLocation.location));
      index.definitionNodes.put(bindingId, node);
    }

    @Override
    public void endVisit(LambdaExpression node) {
      indexDef(node, node.resolveMethodBinding());
    }

    @Override
    public void endVisit(MethodDeclaration node) {
      indexDef(node, node.resolveBinding());
    }

    @Override
    public void endVisit(TypeDeclaration node) {
      indexDef(node, node.resolveBinding());
    }

    @Override
    public void endVisit(SingleVariableDeclaration node) {
      indexDef(node, node.resolveBinding());
    }

    @Override
    public void endVisit(VariableDeclarationFragment node) {
      if (!(node.getParent() instanceof FieldDeclaration)) {
        indexDef(node, node.resolveBinding());
      }
    }

    @Override
    public void endVisit(FieldDeclaration node) {
      final List fragments = node.fragments();
      for (Object fragment : fragments) {
        if (fragment instanceof VariableDeclarationFragment) {
          indexDef(node, ((VariableDeclarationFragment) fragment).resolveBinding());
        } else {
          if (isVerbose()) logger.info(String.format("Other fragment type %s at %s", fragment.getClass().getSimpleName(), getLocation(node)));
        }
      }
    }

    @Override
    public void endVisit(QualifiedName node) {
      final IBinding binding = node.resolveBinding();
      if (null != binding) {
        indexReference(node, binding);
      }
      super.endVisit(node);
    }

    public void indexReference(Name node, IBinding binding) {
      if (null != binding) {
        BindingId bindingId = index.describe(binding);
        final ContextLocation contextLocation = index.getContextLocation(node, this::getSpan);
        final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
        if (isVerbose()) logger.info(String.format("Reference to %s at %s within:\n\t%s", bindingId, contextLocation.location, contextPath));
        index.references.computeIfAbsent(bindingId, x -> new ArrayList<>()).add(contextLocation);
      } else {
        if (isVerbose()) logger.info(String.format("Unresolved element for %s at %s", binding.getName(), getLocation(node)));
      }
    }

    @Override
    public void endVisit(SimpleName node) {
      if (!(node.getParent() instanceof QualifiedName)) {
        final IBinding binding = node.resolveBinding();
        if (null != binding) {
          indexReference(node, binding);
        }
      }
      super.endVisit(node);
    }

    public boolean isVerbose() {
      return verbose;
    }

    public IndexSymbols setVerbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }
  }

  public static class BindingId {
    public final String path;
    public final String type;

    public BindingId(String path, String type) {
      this.path = path;
      this.type = type;
    }

    @Override
    public String toString() {
      return String.format("%s %s", type, path);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BindingId bindingId = (BindingId) o;
      return Objects.equals(path, bindingId.path) &&
          Objects.equals(type, bindingId.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, type);
    }

    public BindingId setType(String type) {
      return new BindingId(path, type);
    }
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
}
