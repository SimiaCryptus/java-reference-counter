package com.simiacryptus.ref.core;

import com.simiacryptus.ref.core.ops.ASTEditor;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

public class SymbolIndex {
  protected static final Logger logger = LoggerFactory.getLogger(SymbolIndex.class);
  public final HashMap<BindingID, ASTNode> definitions = new HashMap<>();
  public final HashMap<BindingID, List<ASTNode>> references = new HashMap<>();

  public static BindingID getBindingID(@Nonnull IBinding binding) {
    if (null == binding) return null;
    final String path = getPath(binding);
    if (null == path) return null;
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
        final IMethodBinding declaringMethod = variableBinding.getDeclaringMethod();
        final String paramName = null == variableBinding ? "?" : new String(localVariableBinding.declaration.name);
        final MethodScope declaringScope = (MethodScope) localVariableBinding.declaringScope;
        if (declaringScope.referenceContext instanceof org.eclipse.jdt.internal.compiler.ast.LambdaExpression) {
          final org.eclipse.jdt.internal.compiler.ast.LambdaExpression lambdaExpression = (org.eclipse.jdt.internal.compiler.ast.LambdaExpression) declaringScope.referenceContext;
          return (Arrays.stream(lambdaExpression.binding.declaringClass.compoundName)
              .map(x -> new String(x)).reduce((a, b) -> a + "." + b).get()
              + "::" + new String(lambdaExpression.binding.selector)) + "::" + paramName;
        } else {
          return getPath(declaringMethod) + "::" + paramName;
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
    if (getPath(binding).matches(".*::lambda\\$\\d+")) {
      return "Lambda";
    }
    if (binding instanceof IVariableBinding) {
      final IVariableBinding variableBinding = (IVariableBinding) binding;
      if (variableBinding.isField()) {
        return "Field";
      } else if (variableBinding.isParameter()) {
        return "Parameter";
      } else {
        return "Variable";
      }
    } else if (binding instanceof IMethodBinding) {
      return "Method";
    } else if (binding instanceof ITypeBinding) {
      if (((ITypeBinding) binding).isAnonymous()) {
        return "Anonymous Class";
      } else {
        return "Type";
      }
    } else {
      return String.format("Other (%s)", binding.getClass().getSimpleName());
    }
  }

  public static class ContextLocation {
    public final ASTEditor.Span location;
    public final LinkedHashMap<BindingID, ASTEditor.Span> context;

    public ContextLocation(ASTEditor.Span location, LinkedHashMap<BindingID, ASTEditor.Span> context) {
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

}
