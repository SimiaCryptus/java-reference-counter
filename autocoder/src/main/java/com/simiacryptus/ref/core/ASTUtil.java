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

public class ASTUtil {

  @Nonnull
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

  public static boolean derives(@Nonnull ITypeBinding typeBinding, @Nonnull Class<?> baseClass) {
    final String binaryName = typeBinding.getTypeDeclaration().getQualifiedName();
    if (null != binaryName && binaryName.equals(baseClass.getCanonicalName())) return true;
    if (Arrays.stream(typeBinding.getInterfaces()).filter(x -> derives(x, baseClass)).findAny().isPresent()) return true;
    if (typeBinding.getSuperclass() != null) return derives(typeBinding.getSuperclass(), baseClass);
    if (typeBinding.isArray()) return derives(typeBinding.getElementType(), baseClass);
    return false;
  }

  @Nonnull
  public static Name newQualifiedName(@Nonnull AST ast, @Nonnull Class<?> clazz) {
    return newQualifiedName(ast, clazz.getName().split("\\."));
  }

  @Nonnull
  public static Name newQualifiedName(@Nonnull AST ast, @Nonnull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(String[]::new)), simpleName);
  }

  @Nonnull
  public static ArrayType arrayType(@Nonnull AST ast, @Nonnull String fqTypeName, int rank) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)), rank);
  }

  @Nonnull
  public static MarkerAnnotation annotation_override(@Nonnull AST ast) {
    return newMarkerAnnotation(ast, "Override");
  }

  @Nonnull
  public static MarkerAnnotation newMarkerAnnotation(@Nonnull AST ast, @Nonnull String name) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName(name));
    return annotation;
  }

  @Nonnull
  public static MarkerAnnotation newMarkerAnnotation(@Nonnull AST ast, @Nonnull Class<?> aClass) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(newQualifiedName(ast, aClass));
    return annotation;
  }

  @Nonnull
  public static SingleMemberAnnotation annotation_SuppressWarnings(@Nonnull AST ast, @Nonnull String label) {
    final SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
    annotation.setTypeName(ast.newSimpleName("SuppressWarnings"));
    final StringLiteral stringLiteral = ast.newStringLiteral();
    stringLiteral.setLiteralValue(label);
    annotation.setValue(stringLiteral);
    return annotation;
  }

  public static boolean hasAnnotation(@Nullable IBinding declaringClass, @Nonnull Class<?> aClass) {
    if (declaringClass == null) return false;
    if (declaringClass.toString().startsWith("Anonymous")) return false;
    return findAnnotation(aClass, declaringClass.getAnnotations()).isPresent();
  }

  @Nonnull
  public static Optional<IAnnotationBinding> findAnnotation(@Nonnull Class<?> aClass, @Nonnull IAnnotationBinding... annotations) {
    return Arrays.stream(annotations)
        .filter(qualifiedName -> qualifiedName.getAnnotationType().getQualifiedName().equals(aClass.getCanonicalName()))
        .findAny();
  }

  @Nonnull
  public static <T extends ASTNode> List<T> findExpressions(@Nonnull ASTNode tree, @Nonnull T searchFor) {
    final List<T> reference = new ArrayList<>();
    final Class<T> searchForClass = (Class<T>) searchFor.getClass();
    final String searchForString = searchFor.toString();
    tree.accept(new ASTVisitor() {
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

  @Nonnull
  public static <T extends ASTNode> List<T> findExpressions(@Nonnull ASTNode tree, @Nonnull Class<T> searchForClass) {
    final List<T> reference = new ArrayList<>();
    tree.accept(new ASTVisitor() {
      @Override
      public void postVisit(@Nonnull ASTNode node) {
        if (node.getClass().equals(searchForClass)) {
          reference.add((T) node);
        }
      }
    });
    return reference;
  }

  @Nonnull
  public static Optional<MethodDeclaration> findMethod(@Nonnull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  @Nonnull
  public static Optional<MethodDeclaration> findMethod(@Nonnull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x -> x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration) methodDeclaration).getName().toString().equals(name)).findFirst();
  }

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

  @Nullable
  public static LambdaExpression getLambda(@Nullable ASTNode node) {
    if (node == null) return null;
    if (node instanceof LambdaExpression) return (LambdaExpression) node;
    return getLambda(node.getParent());
  }

  @Nullable
  public static Statement getStatement(@Nullable ASTNode node) {
    if (node == null) return null;
    if (node instanceof Statement) return (Statement) node;
    return getStatement(node.getParent());
  }

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

  public static boolean isField(@Nonnull SimpleName simpleName) {
    final IBinding iBinding = simpleName.resolveBinding();
    final boolean isVariable = iBinding instanceof IVariableBinding;
    boolean isField = false;
    if (isVariable) {
      isField = ReflectionUtil.getField(iBinding, "binding") instanceof FieldBinding;
    }
    return isField;
  }

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

  public static boolean isExit(Statement statement) {
    if (statement instanceof ReturnStatement) return true;
    if (statement instanceof ThrowStatement) return true;
    return false;
  }

  public static boolean contains(@Nonnull ASTNode node, @Nonnull ASTNode searchFor) {
    return !findExpressions(node, searchFor).isEmpty();
  }

  public static boolean contains(@Nonnull ASTNode node, @Nonnull Class<? extends ASTNode> searchFor) {
    return !findExpressions(node, searchFor).isEmpty();
  }

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

  @Nonnull
  public static DefaultCodeFormatterOptions formattingSettings() {
    final DefaultCodeFormatterOptions javaConventionsSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
    javaConventionsSettings.align_with_spaces = true;
    javaConventionsSettings.tab_char = DefaultCodeFormatterOptions.SPACE;
    javaConventionsSettings.indentation_size = 2;
    return javaConventionsSettings;
  }

  public static boolean isPrimitive(@Nonnull ITypeBinding type) {
    if (type.isArray()) return isPrimitive(type.getElementType());
    return type.isPrimitive();
  }

  public static List<IMethodBinding> superMethods(@Nonnull IMethodBinding methodBinding) {
    final IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
    return superTypes(methodDeclaration.getDeclaringClass())
        .stream()
        .map(ITypeBinding::getDeclaredMethods)
        .flatMap(Arrays::stream)
        .filter(methodDeclaration::overrides)
        .distinct()
        .collect(Collectors.toList());
  }

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

  @Nullable
  public static Tuple2<ASTNode, IMethodBinding> getMethodTuplet(@Nullable ASTNode node) {
    if (null == node) return null;
    if (node instanceof MethodDeclaration) return new Tuple2<>(node, ((MethodDeclaration) node).resolveBinding());
    if (node instanceof LambdaExpression) return new Tuple2<>(node, ((LambdaExpression) node).resolveMethodBinding());
    return getMethodTuplet(node.getParent());
  }

  @Nullable
  public static MethodDeclaration getMethod(@Nullable ASTNode node) {
    if (null == node) return null;
    if (node instanceof MethodDeclaration) return (MethodDeclaration) node;
    return getMethod(node.getParent());
  }

  @Nonnull
  public static <T> List<T> copyPrepend(@Nonnull List<T> list, @Nonnull T... item) {
    ArrayList<T> copy = new ArrayList<>();
    Arrays.stream(item).forEach(copy::add);
    copy.addAll(list);
    return copy;
  }

  @Nonnull
  public static <T> List<T> copyAppend(@Nonnull List<T> list, @Nonnull T... item) {
    ArrayList<T> copy = new ArrayList<>();
    copy.addAll(list);
    Arrays.stream(item).forEach(copy::add);
    return copy;
  }

  public static boolean withinAnonymousClass(ASTNode code, @Nullable ASTNode node) {
    if (null == node) return false;
    if (node instanceof AnonymousClassDeclaration) return true;
    if (code == node) return false;
    return withinAnonymousClass(code, node.getParent());
  }

  public static boolean withinSubMethod(ASTNode code, @Nullable ASTNode node) {
    if (null == node) return false;
    if (node instanceof MethodDeclaration) return true;
    if (code == node) return false;
    return withinSubMethod(code, node.getParent());
  }

  public static boolean withinLambda(ASTNode code, @Nullable ASTNode node) {
    if (null == node) return false;
    if (node instanceof LambdaExpression) return true;
    if (code == node) return false;
    return withinLambda(code, node.getParent());
  }

  public static boolean isLoopTerminal(@Nonnull WhileStatement whileStatement) {
    if (whileStatement.getExpression().toString().equals("true")) {
      if (!contains(whileStatement.getBody(), BreakStatement.class)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isLoopTerminal(@Nonnull DoStatement doStatement) {
    if (doStatement.getExpression().toString().equals("true")) {
      if (!contains(doStatement.getBody(), BreakStatement.class)) {
        return true;
      }
    }
    return false;
  }

  public static boolean strEquals(@Nullable ASTNode l, @Nullable ASTNode r) {
    if (null == r && null == l) return true;
    if (null == r) return false;
    if (null == l) return false;
    return r.toString().equals(l.toString());
  }
}
