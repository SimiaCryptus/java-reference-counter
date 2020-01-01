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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Ast util.
 */
public class ASTUtil {

  /**
   * Align ast editor . ast mapping.
   *
   * @param from the from
   * @param to   the to
   * @return the ast editor . ast mapping
   */
  @NotNull
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
   * Children linked hash map.
   *
   * @param node the node
   * @return the linked hash map
   */
  @NotNull
  public static LinkedHashMap<String, Object> children(@NotNull ASTNode node) {
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
   * Derives boolean.
   *
   * @param typeBinding the type binding
   * @param baseClass   the base class
   * @return the boolean
   */
  public static boolean derives(@Nonnull ITypeBinding typeBinding, @Nonnull Class<?> baseClass) {
    final String binaryName = typeBinding.getTypeDeclaration().getQualifiedName();
    if (null != binaryName && binaryName.equals(baseClass.getCanonicalName())) return true;
    if (Arrays.stream(typeBinding.getInterfaces()).filter(x -> derives(x, baseClass)).findAny().isPresent()) return true;
    if (typeBinding.getSuperclass() != null) return derives(typeBinding.getSuperclass(), baseClass);
    if (typeBinding.isArray()) return derives(typeBinding.getElementType(), baseClass);
    return false;
  }

  /**
   * New qualified name name.
   *
   * @param ast   the ast
   * @param clazz the clazz
   * @return the name
   */
  @NotNull
  public static Name newQualifiedName(@NotNull AST ast, @NotNull Class<?> clazz) {
    return newQualifiedName(ast, clazz.getName().split("\\."));
  }

  /**
   * New qualified name name.
   *
   * @param ast  the ast
   * @param path the path
   * @return the name
   */
  @NotNull
  public static Name newQualifiedName(@NotNull AST ast, @NotNull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(String[]::new)), simpleName);
  }

  /**
   * Array type array type.
   *
   * @param ast        the ast
   * @param fqTypeName the fq type name
   * @param rank       the rank
   * @return the array type
   */
  @NotNull
  public static ArrayType arrayType(@NotNull AST ast, @NotNull String fqTypeName, int rank) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)), rank);
  }

  /**
   * Annotation override marker annotation.
   *
   * @param ast the ast
   * @return the marker annotation
   */
  @NotNull
  public static MarkerAnnotation annotation_override(@NotNull AST ast) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName("Override"));
    return annotation;
  }

  /**
   * Annotation suppress warnings single member annotation.
   *
   * @param ast   the ast
   * @param label the label
   * @return the single member annotation
   */
  @NotNull
  public static SingleMemberAnnotation annotation_SuppressWarnings(@NotNull AST ast, @NotNull String label) {
    final SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
    annotation.setTypeName(ast.newSimpleName("SuppressWarnings"));
    final StringLiteral stringLiteral = ast.newStringLiteral();
    stringLiteral.setLiteralValue(label);
    annotation.setValue(stringLiteral);
    return annotation;
  }

  /**
   * Has annotation boolean.
   *
   * @param declaringClass the declaring class
   * @param aClass         the a class
   * @return the boolean
   */
  public static boolean hasAnnotation(@Nullable IBinding declaringClass, @NotNull Class<?> aClass) {
    if (declaringClass == null) return false;
    if (declaringClass.toString().startsWith("Anonymous")) return false;
    return Arrays.stream(declaringClass.getAnnotations())
        .map(annotation -> annotation.getAnnotationType().getQualifiedName())
        .anyMatch(qualifiedName -> qualifiedName.equals(aClass.getCanonicalName()));
  }

  /**
   * Find expressions list.
   *
   * @param <T>       the type parameter
   * @param tree      the tree
   * @param searchFor the search for
   * @return the list
   */
  @NotNull
  public static <T extends ASTNode> List<T> findExpressions(@NotNull ASTNode tree, @NotNull T searchFor) {
    final List<T> reference = new ArrayList<>();
    final Class<T> searchForClass = (Class<T>) searchFor.getClass();
    final String searchForString = searchFor.toString();
    tree.accept(new ASTVisitor() {
      @Override
      public void postVisit(@NotNull ASTNode node) {
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
   * Find method optional.
   *
   * @param typeDeclaration the type declaration
   * @param name            the name
   * @return the optional
   */
  @NotNull
  public static Optional<MethodDeclaration> findMethod(@NotNull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  /**
   * Find method optional.
   *
   * @param typeDeclaration the type declaration
   * @param name            the name
   * @return the optional
   */
  @NotNull
  public static Optional<MethodDeclaration> findMethod(@NotNull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x -> x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration) methodDeclaration).getName().toString().equals(name)).findFirst();
  }

  /**
   * Gets block.
   *
   * @param node the node
   * @return the block
   */
  @Nullable
  public static Block getBlock(@NotNull ASTNode node) {
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
   * Gets lambda.
   *
   * @param node the node
   * @return the lambda
   */
  @Nullable
  public static LambdaExpression getLambda(@Nullable ASTNode node) {
    if (node == null) return null;
    if (node instanceof LambdaExpression) return (LambdaExpression) node;
    return getLambda(node.getParent());
  }

  /**
   * Gets statement.
   *
   * @param node the node
   * @return the statement
   */
  @Nullable
  public static Statement getStatement(@Nullable ASTNode node) {
    if (node == null) return null;
    if (node instanceof Statement) return (Statement) node;
    return getStatement(node.getParent());
  }

  /**
   * Is evaluable boolean.
   *
   * @param node the node
   * @return the boolean
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
   * Is field boolean.
   *
   * @param simpleName the simple name
   * @return the boolean
   */
  public static boolean isField(@NotNull SimpleName simpleName) {
    final IBinding iBinding = simpleName.resolveBinding();
    final boolean isVariable = iBinding instanceof IVariableBinding;
    boolean isField = false;
    if (isVariable) {
      isField = ReflectionUtil.getField(iBinding, "binding") instanceof FieldBinding;
    }
    return isField;
  }

  /**
   * Update content string.
   *
   * @param content the content
   * @param cu      the cu
   * @return the string
   */
  public static String updateContent(String content, @NotNull CompilationUnit cu) {
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
   * Is exit boolean.
   *
   * @param statement the statement
   * @return the boolean
   */
  public static boolean isExit(Statement statement) {
    if (statement instanceof ReturnStatement) return true;
    if (statement instanceof ThrowStatement) return true;
    return false;
  }

  /**
   * Contains boolean.
   *
   * @param node      the node
   * @param searchFor the search for
   * @return the boolean
   */
  public static boolean contains(@NotNull ASTNode node, ASTNode searchFor) {
    final AtomicBoolean returnValue = new AtomicBoolean(false);
    node.accept(new ASTVisitor() {
      @Override
      public void postVisit(@NotNull ASTNode node) {
        if (node.equals(searchFor)) returnValue.set(true);
        super.postVisit(node);
      }
    });
    return returnValue.get();
  }

  /**
   * Format string.
   *
   * @param finalSrc the final src
   * @return the string
   */
  public static String format(@NotNull String finalSrc) {
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

  /**
   * Formatting settings default code formatter options.
   *
   * @return the default code formatter options
   */
  @NotNull
  public static DefaultCodeFormatterOptions formattingSettings() {
    final DefaultCodeFormatterOptions javaConventionsSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
    javaConventionsSettings.align_with_spaces = true;
    javaConventionsSettings.tab_char = DefaultCodeFormatterOptions.SPACE;
    javaConventionsSettings.indentation_size = 2;
    return javaConventionsSettings;
  }
}
