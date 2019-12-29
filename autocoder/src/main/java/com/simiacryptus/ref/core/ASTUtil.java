package com.simiacryptus.ref.core;

import com.simiacryptus.ref.core.ops.ASTEditor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class ASTUtil {

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

  public static LinkedHashMap<String, Object> children(ASTNode node) {
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

  @NotNull
  public static Name newQualifiedName(AST ast, Class<?> clazz) {
    return newQualifiedName(ast, clazz.getName().split("\\."));
  }

  @NotNull
  public static Name newQualifiedName(@NotNull AST ast, @NotNull String... path) {
    final SimpleName simpleName = ast.newSimpleName(path[path.length - 1]);
    if (path.length == 1) return simpleName;
    return ast.newQualifiedName(newQualifiedName(ast, Arrays.stream(path).limit(path.length - 1).toArray(String[]::new)), simpleName);
  }

  @NotNull
  public static ArrayType arrayType(@NotNull AST ast, @NotNull String fqTypeName, int rank) {
    return ast.newArrayType(ast.newSimpleType(ast.newSimpleName(fqTypeName)), rank);
  }

  @NotNull
  public static MarkerAnnotation annotation_override(@NotNull AST ast) {
    final MarkerAnnotation annotation = ast.newMarkerAnnotation();
    annotation.setTypeName(ast.newSimpleName("Override"));
    return annotation;
  }

  public static boolean hasAnnotation(IBinding declaringClass, Class<?> aClass) {
    if (declaringClass == null) return false;
    if (declaringClass.toString().startsWith("Anonymous")) return false;
    return Arrays.stream(declaringClass.getAnnotations())
        .map(annotation -> annotation.getAnnotationType().getQualifiedName())
        .anyMatch(qualifiedName -> qualifiedName.equals(aClass.getCanonicalName()));
  }

  public static <T extends ASTNode> List<T> findExpressions(ASTNode tree, T searchFor) {
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

  public static Optional<MethodDeclaration> findMethod(@NotNull TypeDeclaration typeDeclaration, String name) {
    return Arrays.stream(typeDeclaration.getMethods()).filter(methodDeclaration -> methodDeclaration.getName().toString().equals(name)).findFirst();
  }

  public static Optional<MethodDeclaration> findMethod(@NotNull AnonymousClassDeclaration typeDeclaration, String name) {
    return typeDeclaration.bodyDeclarations().stream()
        .filter(x -> x instanceof MethodDeclaration)
        .filter(methodDeclaration -> ((MethodDeclaration) methodDeclaration).getName().toString().equals(name)).findFirst();
  }

  @Nullable
  public static Block getBlock(ASTNode node) {
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

  public static LambdaExpression getLambda(ASTNode node) {
    if (node == null) return null;
    if (node instanceof LambdaExpression) return (LambdaExpression) node;
    return getLambda(node.getParent());
  }

  public static Statement getStatement(ASTNode node) {
    if (node == null) return null;
    if (node instanceof Statement) return (Statement) node;
    return getStatement(node.getParent());
  }

  public static boolean isEvaluable(Expression node) {
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

  public static boolean isField(SimpleName simpleName) {
    final IBinding iBinding = simpleName.resolveBinding();
    final boolean isVariable = iBinding instanceof IVariableBinding;
    boolean isField = false;
    if (isVariable) {
      isField = ReflectionUtil.getField(iBinding, "binding") instanceof FieldBinding;
    }
    return isField;
  }

  public static String updateContent(String content, CompilationUnit cu) {
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
}
