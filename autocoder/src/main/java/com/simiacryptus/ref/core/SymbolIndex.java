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
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class SymbolIndex {
  protected static final Logger logger = LoggerFactory.getLogger(SymbolIndex.class);
  public final HashMap<BindingID, ASTNode> definitions = new HashMap<>();
  public final HashMap<BindingID, List<ASTNode>> references = new HashMap<>();

  @Nullable
  public static BindingID getBindingID(@Nonnull IBinding binding) {
    final String path = getPath(binding);
    if (null == path) return null;
    else return new BindingID(path, getType(binding));
  }

  public static boolean equals(@Nonnull IBinding nameBinding, @Nonnull IBinding mentionBinding) {
    return getBindingID(mentionBinding).equals(getBindingID(nameBinding));
  }

  @Nonnull
  private static IMethodBinding getImplementation(@Nonnull IMethodBinding methodBinding) {
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
        final ITypeBinding declaringClass = variableBinding.getDeclaringClass();
        return String.format("%s::%s",
            null == declaringClass ? "null" : getPath(declaringClass),
            variableBinding.getName());
      } else if (variableBinding.isParameter()) {
        final IMethodBinding declaringMethod = variableBinding.getDeclaringMethod();
        return String.format("%s::%s",
            null == declaringMethod ? "null" : getPath(declaringMethod),
            variableBinding.getName());
      } else {
        final IMethodBinding declaringMethod = variableBinding.getDeclaringMethod();
        return String.format("%s::%s[%s]",
            null == declaringMethod ? "null" : getPath(declaringMethod),
            variableBinding.getName(),
            variableBinding.getVariableId());
      }
    } else if (binding instanceof IMethodBinding) {
      IMethodBinding methodBinding = getImplementation((IMethodBinding) binding);
      final ITypeBinding declaringClass = methodBinding.getDeclaringClass();
      return String.format("%s::%s",
          null == declaringClass ? "null" : getPath(declaringClass),
          methodName(methodBinding));
    } else if (binding instanceof ITypeBinding) {
      final ITypeBinding typeBinding = (ITypeBinding) binding;
      if (typeBinding.isAnonymous()) {
        final String[] split = typeBinding.getKey().split("~");
        final ITypeBinding declaringClass = typeBinding.getDeclaringClass();
        return String.format("%s.%s",
            null == declaringClass ? "null" : getPath(declaringClass),
            split.length < 2 ? typeBinding.getKey() : split[1]);
      } else {
        return typeBinding.getTypeDeclaration().getQualifiedName();
      }
    } else if (binding instanceof IPackageBinding) {
      return binding.getName();
    } else {
      final String msg = String.format("Cannot format path of %s", null == binding ? "null" : binding.getClass().getCanonicalName());
      logger.warn(msg);
      throw new RuntimeException(msg);
    }
  }

  private static String methodName(@Nullable IMethodBinding methodBinding) {
    if (null == methodBinding) return "null";
    return String.format("%s(%s)",
        methodBinding.getName(),
        Arrays.stream(methodBinding.getParameterTypes()).map(x -> x.getQualifiedName()).reduce((a, b) -> a + "," + b).orElse("")
    );
  }

  private static String getType(IBinding binding) {
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

  @Nonnull
  public LinkedHashMap<BindingID, ASTNode> context(@Nonnull ASTNode node) {
    final LinkedHashMap<BindingID, ASTNode> list = new LinkedHashMap<>();
    final ASTNode parent = node.getParent();
    if (parent != null) list.putAll(this.context(parent));
    if (node instanceof LambdaExpression) {
      final IMethodBinding methodBinding = ((LambdaExpression) node).resolveMethodBinding();
      if (methodBinding == null) {
        logger.warn("Unresolved binding for %s", node);
      } else {
        list.put(getBindingID(methodBinding), node);
      }
    } else if (node instanceof MethodDeclaration) {
      final IMethodBinding methodBinding = ((MethodDeclaration) node).resolveBinding();
      if (methodBinding == null) {
        logger.warn("Unresolved binding for %s", node);
      } else {
        list.put(getBindingID(methodBinding), node);
      }
    } else if (node instanceof TypeDeclaration) {
      final ITypeBinding typeBinding = ((TypeDeclaration) node).resolveBinding();
      if (typeBinding == null) {
        logger.warn("Unresolved binding for %s", node);
      } else {
        list.put(getBindingID(typeBinding), node);
      }
    }
    return list;
  }

  public static class ContextLocation {
    public final ASTEditor.Span location;
    public final @Nonnull
    LinkedHashMap<BindingID, ASTNode> context;

    public ContextLocation(ASTEditor.Span location, @Nonnull LinkedHashMap<BindingID, ASTNode> context) {
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

    @Nonnull
    @SuppressWarnings("unused")
    public BindingID setType(String type) {
      return new BindingID(path, type);
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
