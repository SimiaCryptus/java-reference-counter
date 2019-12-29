package com.simiacryptus.ref.core;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

public class SymbolIndex {
  protected static final Logger logger = LoggerFactory.getLogger(SymbolIndex.class);
  public final HashMap<BindingID, ContextLocation> definitionLocations = new HashMap<>();
  public final HashMap<BindingID, ASTNode> definitionNodes = new HashMap<>();
  public final HashMap<BindingID, List<ContextLocation>> references = new HashMap<>();

  public static BindingID getBindingID(@Nonnull IBinding binding) {
    if (null == binding) return null;
    final String path = getPath(binding);
    if (null == path) return null;
    if (path.contains("::lambda$")) return new BindingID(path, "Lambda");
    else return new BindingID(path, getType(binding));
  }

  private static IMethodBinding getImplementation(IMethodBinding methodBinding) {
    while (true) {
      IMethodBinding impl = ReflectionUtil.getField(methodBinding, "implementation");
      if (null != impl && methodBinding != impl) {
        methodBinding = impl;
      } else break;
    }
    return methodBinding;
  }

  private static String getPath(IBinding binding) {
    if (binding instanceof IVariableBinding) {
      IVariableBinding variableBinding = (IVariableBinding) binding;
      if (variableBinding.isField()) {
        final FieldBinding fieldBinding = ReflectionUtil.getField(variableBinding, "binding");
        if (fieldBinding != null) {
          final ReferenceBinding declaringClass = fieldBinding.declaringClass;
          if (null == declaringClass) {
            return null;
          }
          final String className = Arrays.stream(declaringClass.compoundName).map(String::new).reduce((a, b) -> a + "." + b).get();
          return String.format("%s::%s",
              className,
              null == variableBinding ? "null" : variableBinding.getName());
        } else {
          return "null::" + variableBinding.getName();
        }
      } else if (variableBinding.isParameter()) {
        final LocalVariableBinding localVariableBinding = ReflectionUtil.getField(variableBinding, "binding");
        final IMethodBinding methodBinding = variableBinding.getDeclaringMethod();
        final String paramName = null == variableBinding ? "?" : new String(localVariableBinding.declaration.name);
        final MethodScope declaringScope = (MethodScope) localVariableBinding.declaringScope;
        if (declaringScope.referenceContext instanceof org.eclipse.jdt.internal.compiler.ast.LambdaExpression) {
          return (Arrays.stream(((org.eclipse.jdt.internal.compiler.ast.LambdaExpression) declaringScope.referenceContext).binding.declaringClass.compoundName)
              .map(x -> new String(x)).reduce((a, b) -> a + "." + b).get()
              + "::" + new String(((org.eclipse.jdt.internal.compiler.ast.LambdaExpression) declaringScope.referenceContext).binding.selector)) + "::" + paramName;
        } else {
          return getPath(methodBinding) + "::" + paramName;
        }
      } else {
        final IMethodBinding declaringMethod = variableBinding.getDeclaringMethod();

        return String.format("%s::%s[%s]",
            null == declaringMethod ? "null" : getPath(declaringMethod),
            null == variableBinding ? "null" : variableBinding.getName(),
            null == variableBinding ? "?" : variableBinding.getVariableId());
      }
    } else if (binding instanceof IMethodBinding) {
      IMethodBinding methodBinding = getImplementation((IMethodBinding) binding);
      final String typeBinding = null == methodBinding ? "null" : getPath(methodBinding.getDeclaringClass());
      return typeBinding + "::" + methodName(methodBinding);
    } else if (binding instanceof ITypeBinding) {
      final ITypeBinding typeBinding = (ITypeBinding) binding;
      if (typeBinding.isAnonymous()) {
        final String[] split = typeBinding.getKey().split("~");
        if (split.length < 2) return getPath(typeBinding.getDeclaringClass()) + "." + typeBinding.getKey();
        return getPath(typeBinding.getDeclaringClass()) + "." + split[1];
      } else {
        return typeBinding.getQualifiedName();
      }
    } else if (binding instanceof IPackageBinding) {
      return binding.getName();
    } else {
      final String msg = String.format("Cannot format path of %s", null == binding ? "null" : binding.getClass().getCanonicalName());
      logger.warn(msg);
      throw new RuntimeException(msg);
    }
  }

  private static String methodName(IMethodBinding methodBinding) {
    if (null == methodBinding) return "null";
    return String.format("%s(%s)",
        methodBinding.getName(),
        Arrays.stream(methodBinding.getParameterTypes()).map(x -> x.getQualifiedName()).reduce((a, b) -> a + "," + b).orElse("")
    );
  }

  private static String getType(IBinding binding) {
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
      if (((ITypeBinding) binding).isAnonymous()) {
        type = "Anonymous Class";
      } else {
        type = "Type";
      }
    } else {
      type = String.format("Other (%s)", binding.getClass().getSimpleName());
    }
    return type;
  }

  public static class ContextLocation {
    public final Span location;
    public final LinkedHashMap<BindingID, Span> context;

    public ContextLocation(Span location, LinkedHashMap<BindingID, Span> context) {
      this.location = location;
      this.context = context;
    }
  }

  public static class BindingID {
    public final String path;
    public final String type;

    public BindingID(String path, String type) {
      this.path = path;
      this.type = type;
    }

    public BindingID setType(String type) {
      return new BindingID(path, type);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BindingID bindingID = (BindingID) o;
      return Objects.equals(path, bindingID.path) &&
          Objects.equals(type, bindingID.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, type);
    }

    @Override
    public String toString() {
      return String.format("%s %s", type, path);
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

    public boolean contains(Span location) {
      if (location.lineStart < lineStart) return false;
      if (location.lineStart == lineStart && location.colStart < colStart) return false;
      if (location.lineEnd > lineEnd) return false;
      if (location.lineEnd == lineEnd && location.colEnd > colEnd) return false;
      return true;
    }

    @Override
    public String toString() {
      return String.format("%s:{%d:%d-%d:%d}", file.getName(), lineStart, colStart, lineEnd, colEnd);
    }
  }
}
