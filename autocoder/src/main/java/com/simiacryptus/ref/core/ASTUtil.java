/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.core;

import com.simiacryptus.ref.core.ops.ASTEditor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The ASTUtil class provides utility methods for manipulating AST nodes.
 *
 * @docgenVersion 9
 */
public class ASTUtil {

  /**
   * Aligns the given ASTNodes.
   *
   * @param from the ASTNode to align
   * @param to   the ASTNode to align to
   * @return the ASTMapping between the nodes
   * @throws IllegalArgumentException if from or to is null
   * @docgenVersion 9
   */
  public static ASTEditor.ASTMapping align(@Nonnull ASTNode from, @Nonnull ASTNode to) {
    final ASTEditor.ASTMapping mapping = new ASTEditor.ASTMapping();
    if (!from.getClass().equals(to.getClass())) {
      final CompilationUnit root = (CompilationUnit) to.getRoot();
      if (from.toString().equals(to.toString())) {
        mapping.errors.add(String.format("%s [%s] does not match class of %s [%s] at line %s",
            from.toString().trim().replaceAll("\\n", "\n\t"), from.getClass().getSimpleName(),
            to.toString().trim().replaceAll("\\n", "\n\t"), to.getClass().getSimpleName(),
            root.getLineNumber(to.getStartPosition())));
        mapping.matches.put(from, to);
        return mapping;
      } else {
        mapping.errors.add(String.format("%s [%s] does not match class of %s [%s] at line %s",
            from.toString().trim().replaceAll("\\n", "\n\t"), from.getClass().getSimpleName(),
            to.toString().trim().replaceAll("\\n", "\n\t"), to.getClass().getSimpleName(),
            root.getLineNumber(to.getStartPosition())));
        mapping.mismatches.put(from, to);
        return mapping;
      }
    }
    final LinkedHashMap<String, Object> fromChildren = children(from);
    final LinkedHashMap<String, Object> toChildren = children(to);
    if (fromChildren.size() != toChildren.size()) {
      final CompilationUnit root = (CompilationUnit) to.getRoot();
      mapping.errors.add(String.format("%s does not match size of %s at line %s",
          from.toString().trim().replaceAll("\\n", "\n\t"),
          to.toString().trim().replaceAll("\\n", "\n\t"),
          root.getLineNumber(to.getStartPosition())));
      mapping.mismatches.put(from, to);
    } else if (!fromChildren.keySet().equals(toChildren.keySet())) {
      final CompilationUnit root = (CompilationUnit) to.getRoot();
      mapping.errors.add(String.format("%s does not match keys of %s at line %s",
          from.toString().trim().replaceAll("\\n", "\n\t"),
          to.toString().trim().replaceAll("\\n", "\n\t"),
          root.getLineNumber(to.getStartPosition())));
      mapping.mismatches.put(from, to);
    } else if (fromChildren.isEmpty() && !from.toString().equals(to.toString())) {
      final CompilationUnit root = (CompilationUnit) to.getRoot();
      mapping.errors.add(String.format("%s does not match content of %s at line %s",
          from.toString().trim().replaceAll("\\n", "\n\t"),
          to.toString().trim().replaceAll("\\n", "\n\t"),
          root.getLineNumber(to.getStartPosition())));
      mapping.mismatches.put(from, to);
    } else {
      mapping.matches.put(from, to);
      fromChildren.forEach((k, v) -> {
        final Object toValue = toChildren.get(k);
        final Object fromValue = fromChildren.get(k);
        if (toValue != null || fromValue != null) {
          if (toValue != null && fromValue != null) {
            if (fromValue instanceof ASTNode) mapping.putAll(align((ASTNode) fromValue, (ASTNode) toValue));
          } else {
            final CompilationUnit root = (CompilationUnit) to.getRoot();
            mapping.errors.add(String.format("%s does not match %s within %s at %s",
                null == fromValue ? null : fromValue.toString().trim().replaceAll("\\n", "\n\t"),
                null == toValue ? null : toValue.toString().trim().replaceAll("\\n", "\n\t"),
                from,
                root.getLineNumber(to.getStartPosition())));
          }
        }
      });
    }
    return mapping;
  }

  /**
   * Returns a LinkedHashMap of the children of the given ASTNode.
   *
   * @param node the ASTNode to get the children of
   * @return a LinkedHashMap of the children of the given ASTNode
   * @docgenVersion 9
   */
  @Nonnull
  public static LinkedHashMap<String, Object> children(@Nonnull ASTNode node) {
    Collection<StructuralPropertyDescriptor> properties = new TreeSet<>(Comparator.comparing(x -> x.toString()));
    properties.addAll(ReflectionUtil.invokeMethod(node, "internalStructuralPropertiesForType", AST.JLS11));
    final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    properties.forEach(property -> {
      if (property.isChildListProperty()) {
        final List list = (List) node.getStructuralProperty(property);
        for (int i = 0; i < list.size(); i++) {
          map.put(String.format("%s[%d]", property, i), list.get(i));
        }
      } else {
        map.put(property.toString(), node.getStructuralProperty(property));
      }
    });
    return map;
  }

  /**
   * Returns true if the given type derives from the given base class.
   *
   * @param typeBinding the type to check
   * @param baseClass   the base class to check against
   * @return true if the given type derives from the given base class, false otherwise
   * @docgenVersion 9
   */
  public static boolean derives(@Nonnull ITypeBinding typeBinding, @Nonnull Class<?> baseClass) {
    final String binaryName = typeBinding.getTypeDeclaration().getQualifiedName();
    if (null != binaryName && binaryName.equals(baseClass.getCanonicalName())) return true;
    if (Arrays.stream(typeBinding.getInterfaces()).filter(x -> derives(x, baseClass)).findAny().isPresent())
      return true;
    if (typeBinding.getSuperclass() != null) return derives(typeBinding.getSuperclass(), baseClass);
    if (typeBinding.isArray()) return derives(typeBinding.getElementType(), baseClass);
    return false;
  }

  /**
   * Creates a new qualified name from the given AST and class.
   *
   * @param ast   the AST to create the qualified name from
   * @param clazz the class to create the qualified name from
   * @return a new qualified name
   * @docgenVersion 9
   */
  @Nonnull
  public static Name newQualifiedName(@Nonnull AST ast, @Nonnull Class<?> clazz) {
    return newQualifiedName(ast, clazz.getName().split("\\."));
  }

  /**
   * Creates a new qualified name node in the given AST.
   *
   * @param ast  the AST in which to create the node
   * @param path the path of the qualified name
   * @return the newly created qualified name node
   * @throws IllegalArgumentException if <code>ast</code> is <code>null</code> or
   *                                  <code>path</code> is <code>null</code> or empty
   * @docgenVersion 9
   */
  @Nonnull
  public static Name newQualifiedName(@Nonnull AST ast, @Nonnull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(value -> new String[value])), simpleName);
  }

  /**
   * Creates an ArrayType node with the given fully-qualified type name and rank.
   *
   * @param ast        the AST object to use to create the node
   * @param fqTypeName the fully-qualified name of the array's component type
   * @param rank       the number of dimensions in the array type
   * @return the newly created ArrayType node
   * @docgenVersion 9
   */
  @Nonnull
  public static ArrayType arrayType(@Nonnull AST ast, @Nonnull String fqTypeName, int rank) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)), rank);
  }

  /**
   * @return a new {@link MarkerAnnotation} with the given name
   * @throws NullPointerException if the given name is null
   * @docgenVersion 9
   */
  @Nonnull
  public static MarkerAnnotation annotation_override(@Nonnull AST ast) {
    return newMarkerAnnotation(ast, "Override");
  }

  /**
   * Creates a new marker annotation node with the given name.
   *
   * @param ast  the AST to create the node in
   * @param name the name of the annotation
   * @return the new marker annotation node
   * @docgenVersion 9
   */
  @Nonnull
  public static MarkerAnnotation newMarkerAnnotation(@Nonnull AST ast, @Nonnull String name) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName(name));
    return annotation;
  }

  /**
   * Creates a new marker annotation.
   *
   * @param ast    the AST to create the annotation in
   * @param aClass the type of the annotation
   * @return the new marker annotation
   * @docgenVersion 9
   */
  @Nonnull
  public static MarkerAnnotation newMarkerAnnotation(@Nonnull AST ast, @Nonnull Class<?> aClass) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(newQualifiedName(ast, aClass));
    return annotation;
  }

  /**
   * @param ast   the AST to search for the annotation in
   * @param label the label of the annotation to find
   * @return the SingleMemberAnnotation node for the given label
   * @throws IllegalArgumentException if the given AST is {@code null} or the given label is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  public static SingleMemberAnnotation annotation_SuppressWarnings(@Nonnull AST ast, @Nonnull String label) {
    final SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
    annotation.setTypeName(ast.newSimpleName("SuppressWarnings"));
    final StringLiteral stringLiteral = ast.newStringLiteral();
    stringLiteral.setLiteralValue(label);
    annotation.setValue(stringLiteral);
    return annotation;
  }

  /**
   * Returns true if the given class has the given annotation.
   *
   * @param declaringClass the class to check
   * @param aClass         the annotation to check for
   * @return true if the given class has the given annotation
   * @docgenVersion 9
   */
  public static boolean hasAnnotation(@Nullable IBinding declaringClass, @Nonnull Class<?> aClass) {
    if (declaringClass == null) return false;
    if (declaringClass.toString().startsWith("Anonymous")) return false;
    return findAnnotation(aClass, declaringClass.getAnnotations()).isPresent();
  }

  /**
   * @param aClass      the class to find the annotation of
   * @param annotations the array of annotations to check
   * @return an optional containing the annotation if found, empty otherwise
   * @docgenVersion 9
   */
  @Nonnull
  public static Optional<IAnnotationBinding> findAnnotation(@Nonnull Class<?> aClass, @Nonnull IAnnotationBinding... annotations) {
    return Arrays.stream(annotations)
        .filter(qualifiedName -> qualifiedName.getAnnotationType().getQualifiedName().equals(aClass.getCanonicalName()))
        .findAny();
  }

  /**
   * @param tree      the ASTNode tree to search
   * @param searchFor the ASTNode to search for
   * @return a list of ASTNodes that match the searchFor parameter
   * @docgenVersion 9
   */
  @Nonnull
  public static <T extends ASTNode> List<T> findExpressions(@Nonnull ASTNode tree, @Nonnull T searchFor) {
    final List<T> reference = new ArrayList<>();
    final Class<T> searchForClass = (Class<T>) searchFor.getClass();
    final String searchForString = searchFor.toString();
    tree.accept(new ASTVisitor() {
      /**
       * This method overrides the postVisit method in ASTNode.
       * If the class of the node equals the searchForClass,
       * and the string of the node equals the searchForString,
       * the node is added to the reference.
       *
       *   @docgenVersion 9
       */
      @Override
      public void postVisit(@Nonnull ASTNode node) {
        if (node.getClass().equals(searchForClass)) {
          if (node.toString().equals(searchForString)) {
            reference.add((T) node);
          }
        }
      }
    });
    return reference;
  }

  /**
   * Searches the given tree for nodes of the given class.
   *
   * @param tree           The tree to search.
   * @param searchForClass The class to search for.
   * @return A list of nodes of the given class.
   * @docgenVersion 9
   */
  @Nonnull
  public static <T extends ASTNode> List<T> findExpressions(@Nonnull ASTNode tree, @Nonnull Class<T> searchForClass) {
    final List<T> reference = new ArrayList<>();
    tree.accept(new ASTVisitor() {
      /**
       * This method overrides the postVisit method in ASTNode.
       * If the class of the ASTNode is the same as the searchForClass,
       * the node is added to the reference.
       *
       *   @docgenVersion 9
       */
      @Override
      public void postVisit(@Nonnull ASTNode node) {
        if (node.getClass().equals(searchForClass)) {
          reference.add((T) node);
        }
      }
    });
    return reference;
  }

  /**
   * Finds the first method with the given name in the given type declaration.
   *
   * @param typeDeclaration the type declaration to search
   * @param name            the name of the method to find
   * @return an optional containing the first method with the given name, or an empty optional if no such method exists
   * @docgenVersion 9
   */
  @Nonnull
  public static Optional<MethodDeclaration> findMethod(@Nonnull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  /**
   * Finds a method with the given name in an anonymous class declaration.
   *
   * @param typeDeclaration The anonymous class declaration to search.
   * @param name            The name of the method to find.
   * @return An optional containing the method declaration if found, or an empty optional if not found.
   * @docgenVersion 9
   */
  @Nonnull
  public static Optional<MethodDeclaration> findMethod(@Nonnull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x -> x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration) methodDeclaration).getName().toString().equals(name)).findFirst();
  }

  /**
   * Returns the block that contains the given node, or null if the node is not contained in a block.
   *
   * @docgenVersion 9
   */
  @Nullable
  public static Block getBlock(@Nonnull ASTNode node) {
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

  /**
   * @return the lambda expression that contains the given node, or null if there is no such expression
   * @docgenVersion 9
   */
  @Nullable
  public static LambdaExpression getLambda(@Nullable ASTNode node) {
    if (node == null) return null;
    if (node instanceof LambdaExpression) return (LambdaExpression) node;
    return getLambda(node.getParent());
  }

  /**
   * Returns the statement that contains the given AST node, or null if the node is not contained in a statement.
   *
   * @param node the AST node
   * @return the containing statement, or null
   * @docgenVersion 9
   */
  @Nullable
  public static Statement getStatement(@Nullable ASTNode node) {
    if (node == null) return null;
    if (node instanceof Statement) return (Statement) node;
    return getStatement(node.getParent());
  }

  /**
   * Returns true if the given node can be evaluated.
   *
   * @param node the node to check
   * @return true if the node can be evaluated, false otherwise
   * @docgenVersion 9
   */
  public static boolean isEvaluable(@Nullable Expression node) {
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
    } else if (node instanceof ArrayAccess) {
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

  /**
   * Returns true if the given SimpleName is a field.
   *
   * @param simpleName the SimpleName to check
   * @return true if the SimpleName is a field, false otherwise
   * @docgenVersion 9
   */
  public static boolean isField(@Nonnull SimpleName simpleName) {
    final IBinding iBinding = simpleName.resolveBinding();
    final boolean isVariable = iBinding instanceof IVariableBinding;
    boolean isField = false;
    if (isVariable) {
      isField = ReflectionUtil.getField(iBinding, "binding") instanceof FieldBinding;
    }
    return isField;
  }

  /**
   * Updates the given content with the changes from the given compilation unit.
   *
   * @param content the content to update
   * @param cu      the compilation unit with the changes
   * @return the updated content
   * @docgenVersion 9
   */
  public static String updateContent(String content, @Nonnull CompilationUnit cu) {
    Document document = new Document(content);
    final Hashtable<String, String> options = JavaCore.getOptions();
    try {
      cu.rewrite(document, options).apply(document);
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
    return document.get();
  }

  /**
   * Returns true if the statement is an exit statement, false otherwise.
   *
   * @param statement the statement to check
   * @return true if the statement is an exit statement, false otherwise
   * @docgenVersion 9
   */
  public static boolean isExit(Statement statement) {
    if (statement instanceof ReturnStatement) return true;
    if (statement instanceof ThrowStatement) return true;
    return false;
  }

  /**
   * Returns true if the node contains the searchFor node.
   *
   * @param node      the node to search
   * @param searchFor the node to search for
   * @return true if the node contains the searchFor node
   * @docgenVersion 9
   */
  public static boolean contains(@Nonnull ASTNode node, @Nonnull ASTNode searchFor) {
    return !findExpressions(node, searchFor).isEmpty();
  }

  /**
   * Returns true if the given node contains the given type of node.
   *
   * @param node      the node to search
   * @param searchFor the type of node to search for
   * @return true if the given node contains the given type of node
   * @docgenVersion 9
   */
  public static boolean contains(@Nonnull ASTNode node, @Nonnull Class<? extends ASTNode> searchFor) {
    return !findExpressions(node, searchFor).isEmpty();
  }

  /**
   * Formats the given string.
   *
   * @param finalSrc the string to format
   * @return the formatted string
   * @docgenVersion 9
   */
  public static String format(@Nonnull String finalSrc) {
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
      throw new RuntimeException(e);
    }
    return document.get();
  }

  /**
   * Returns the default code formatter options.
   *
   * @return the default code formatter options
   * @docgenVersion 9
   */
  @Nonnull
  public static DefaultCodeFormatterOptions formattingSettings() {
    final DefaultCodeFormatterOptions javaConventionsSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
    javaConventionsSettings.align_with_spaces = true;
    javaConventionsSettings.tab_char = DefaultCodeFormatterOptions.SPACE;
    javaConventionsSettings.indentation_size = 2;
    return javaConventionsSettings;
  }

  /**
   * Returns true if the type is a primitive type, false otherwise.
   *
   * @param type the type to check
   * @return true if the type is a primitive type, false otherwise
   * @docgenVersion 9
   */
  public static boolean isPrimitive(@Nonnull ITypeBinding type) {
    if (type.isArray()) return isPrimitive(type.getElementType());
    return type.isPrimitive();
  }

  /**
   * Returns a list of IMethodBinding objects for all methods that this method overrides.
   *
   * @param methodBinding the IMethodBinding object to get super methods for
   * @return a list of IMethodBinding objects for all methods that this method overrides
   * @docgenVersion 9
   */
  public static List<IMethodBinding> superMethods(@Nonnull IMethodBinding methodBinding) {
    final IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
    return superTypes(methodDeclaration.getDeclaringClass())
        .stream()
        .map(iTypeBinding -> iTypeBinding.getDeclaredMethods())
        .flatMap(array -> Arrays.stream(array))
        .filter(method -> methodDeclaration.overrides(method))
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Returns the list of super types for the given type binding.
   *
   * @param declaringClass the type binding for which to return the list of super types
   * @return the list of super types for the given type binding
   * @docgenVersion 9
   */
  @Nonnull
  public static List<ITypeBinding> superTypes(@Nonnull ITypeBinding declaringClass) {
    final ArrayList<ITypeBinding> list = new ArrayList<>();
    for (ITypeBinding xface : declaringClass.getInterfaces()) {
      list.add(xface);
    }
    final ITypeBinding superclass = declaringClass.getSuperclass();
    if (null != superclass) {
      list.add(superclass);
      list.addAll(superTypes(superclass));
    }
    return list;
  }

  /**
   * Returns a tuple containing an ASTNode and an IMethodBinding, or null if no such tuple exists.
   *
   * @param node the ASTNode to use
   * @return a Tuple2 containing an ASTNode and an IMethodBinding, or null
   * @docgenVersion 9
   */
  @Nullable
  public static Tuple2<ASTNode, IMethodBinding> getMethodTuplet(@Nullable ASTNode node) {
    if (null == node) return null;
    if (node instanceof MethodDeclaration) return new Tuple2<>(node, ((MethodDeclaration) node).resolveBinding());
    if (node instanceof LambdaExpression) return new Tuple2<>(node, ((LambdaExpression) node).resolveMethodBinding());
    return getMethodTuplet(node.getParent());
  }

  /**
   * Returns the MethodDeclaration node for the given ASTNode, or null if not found.
   *
   * @param node the ASTNode to search for a MethodDeclaration
   * @return the MethodDeclaration node for the given ASTNode, or null if not found
   * @docgenVersion 9
   */
  @Nullable
  public static MethodDeclaration getMethod(@Nullable ASTNode node) {
    if (null == node) return null;
    if (node instanceof MethodDeclaration) return (MethodDeclaration) node;
    return getMethod(node.getParent());
  }

  /**
   * Copies the given list and prepends the given items to the copy.
   *
   * @param list the list to copy
   * @param item the items to prepend to the copy
   * @return the copy of the list with the given items prepended
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> List<T> copyPrepend(@Nonnull List<T> list, @Nonnull T... item) {
    ArrayList<T> copy = new ArrayList<>();
    Arrays.stream(item).forEach(e -> copy.add(e));
    copy.addAll(list);
    return copy;
  }

  /**
   * Returns a new list that is a copy of the given list with the given items appended to the end.
   *
   * @param list the list to copy and append to
   * @param item the items to append to the end of the list
   * @return a new list that is a copy of the given list with the given items appended to the end
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> List<T> copyAppend(@Nonnull List<T> list, @Nonnull T... item) {
    ArrayList<T> copy = new ArrayList<>();
    copy.addAll(list);
    Arrays.stream(item).forEach(e -> copy.add(e));
    return copy;
  }

  /**
   * Returns true if the given node is within an anonymous class, false otherwise.
   *
   * @param code the code to check
   * @param node the node to check
   * @return true if the given node is within an anonymous class, false otherwise
   * @docgenVersion 9
   */
  public static boolean withinAnonymousClass(ASTNode code, @Nullable ASTNode node) {
    if (null == node) return false;
    if (node instanceof AnonymousClassDeclaration) return true;
    if (code == node) return false;
    return withinAnonymousClass(code, node.getParent());
  }

  /**
   * Returns true if the given node is within a submethod of the given code.
   *
   * @param code the code to check
   * @param node the node to check
   * @return true if the node is within a submethod of the code
   * @docgenVersion 9
   */
  public static boolean withinSubMethod(ASTNode code, @Nullable ASTNode node) {
    if (null == node) return false;
    if (node instanceof MethodDeclaration) return true;
    if (code == node) return false;
    return withinSubMethod(code, node.getParent());
  }

  /**
   * Returns true if the given node is within a lambda expression, false otherwise.
   *
   * @param code the code to check
   * @param node the node to check
   * @return true if the given node is within a lambda expression, false otherwise
   * @docgenVersion 9
   */
  public static boolean withinLambda(ASTNode code, @Nullable ASTNode node) {
    if (null == node) return false;
    if (node instanceof LambdaExpression) return true;
    if (code == node) return false;
    return withinLambda(code, node.getParent());
  }

  /**
   * Determines if a while loop is terminal.
   *
   * @param whileStatement the while loop statement
   * @return true if the while loop is terminal, false otherwise
   * @docgenVersion 9
   */
  public static boolean isLoopTerminal(@Nonnull WhileStatement whileStatement) {
    if (whileStatement.getExpression().toString().equals("true")) {
      if (!contains(whileStatement.getBody(), BreakStatement.class)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the given do statement is a loop terminal. A loop terminal is a do statement whose expression is "true" and whose body does not contain a break statement.
   *
   * @param doStatement the do statement to check
   * @return true if the given do statement is a loop terminal, false otherwise
   * @docgenVersion 9
   */
  public static boolean isLoopTerminal(@Nonnull DoStatement doStatement) {
    if (doStatement.getExpression().toString().equals("true")) {
      if (!contains(doStatement.getBody(), BreakStatement.class)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param l the left ASTNode to compare
   * @param r the right ASTNode to compare
   * @return true if the two ASTNodes are equal, false otherwise
   * @docgenVersion 9
   */
  public static boolean strEquals(@Nullable ASTNode l, @Nullable ASTNode r) {
    if (null == r && null == l) return true;
    if (null == r) return false;
    if (null == l) return false;
    return r.toString().equals(l.toString());
  }
}
