/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ProjectInfo;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.function.Function;

public class IndexSymbols extends FileAstScanner {

  SymbolIndex index;
  private boolean verbose = true;

  public IndexSymbols(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, SymbolIndex index) {
    super(projectInfo, compilationUnit, file, false);
    this.index = index;
  }

  public LinkedHashMap<BindingId, Span> context(ASTNode node, Function<ASTNode, Span> locator) {
    final LinkedHashMap<BindingId, Span> list = new LinkedHashMap<>();
    final ASTNode parent = node.getParent();
    if (parent != null) list.putAll(context(parent, locator));
    if (node instanceof MethodDeclaration) {
      final MethodDeclaration methodDeclaration = (MethodDeclaration) node;
      list.put(index.describe(resolveBinding(methodDeclaration)), locator.apply(node));
    } else if (node instanceof LambdaExpression) {
      final LambdaExpression lambdaExpression = (LambdaExpression) node;
      final IMethodBinding methodBinding = resolveMethodBinding(lambdaExpression);
      if (methodBinding == null) {
        warn(node, "Unresolved binding for %s", node);
      } else {
        list.put(index.describe(methodBinding).setType("Lambda"), locator.apply(node));
      }
    } else if (node instanceof TypeDeclaration) {
      final TypeDeclaration typeDeclaration = (TypeDeclaration) node;
      final ITypeBinding typeBinding = resolveBinding(typeDeclaration);
      if (typeBinding == null) {
        warn(node, "Unresolved binding for %s", node);
      } else {
        list.put(index.describe(typeBinding), locator.apply(node));
      }
    }
    return list;
  }

  @Override
  public void endVisit(LambdaExpression node) {
    indexDef(node, resolveMethodBinding(node));
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    indexDef(node, resolveBinding(node));
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    indexDef(node, resolveBinding(node));
  }

  @Override
  public void endVisit(SingleVariableDeclaration node) {
    indexDef(node, resolveBinding(node));
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    if (!(node.getParent() instanceof FieldDeclaration)) {
      indexDef(node, resolveBinding(node));
    }
  }

  @Override
  public void endVisit(FieldDeclaration node) {
    final List fragments = node.fragments();
    for (Object fragment : fragments) {
      if (fragment instanceof VariableDeclarationFragment) {
        final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
        indexDef(node, resolveBinding(variableDeclarationFragment));
      } else {
        if (isVerbose()) info(node, "Other fragment type %s", fragment.getClass().getSimpleName());
      }
    }
  }

  @Override
  public void endVisit(QualifiedName node) {
    final ITypeBinding qualifierType = resolveTypeBinding(node.getQualifier());
    if (null != qualifierType && qualifierType.isArray()) return;
    final IBinding binding = resolveBinding(node);
    if (null != binding) {
      indexReference(node, binding);
    }
    super.endVisit(node);
  }

  @Override
  public void endVisit(SimpleName node) {
    if (!(node.getParent() instanceof QualifiedName)) {
      final IBinding binding = resolveBinding(node);
      if (null != binding) {
        indexReference(node, binding);
      }
    }
    super.endVisit(node);
  }

  @NotNull
  public ContextLocation getContextLocation(ASTNode node, Function<ASTNode, Span> locator) {
    return new ContextLocation(locator.apply(node), context(node, locator));
  }

  private void indexDef(ASTNode node, IBinding binding) {
    if (null == binding) return;
    final ContextLocation contextLocation = getContextLocation(node, this::getSpan);
    final BindingId bindingId = index.describe(binding);
    final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
    if (isVerbose()) info(node, "Declaration of %s at %s within: \n\t%s", bindingId, getSpan(node), contextPath);
    final ContextLocation replaced = index.definitionLocations.put(bindingId, contextLocation);
    if (null != replaced) {
      warn(node, "Duplicate declaration of %s in %s and %s", bindingId, replaced.location, contextLocation.location);
    }
    index.definitionNodes.put(bindingId, node);
  }

  public void indexReference(Name node, IBinding binding) {
    if (null != binding) {
      BindingId bindingId = index.describe(binding);
      if (null != bindingId) {
        final ContextLocation contextLocation = getContextLocation(node, this::getSpan);
        final String contextPath = contextLocation.context.entrySet().stream().map(e -> e.getKey() + " at " + e.getValue()).reduce((a, b) -> a + "\n\t" + b).orElse("-");
        if (isVerbose()) info(node, "Reference to %s at %s within:\n\t%s", bindingId, contextLocation.location, contextPath);
        index.references.computeIfAbsent(bindingId, x -> new ArrayList<>()).add(contextLocation);
      }
    } else {
      if (isVerbose()) info(node, "Unresolved element for %s", binding.getName());
    }
  }

  public boolean isVerbose() {
    return verbose;
  }

  public IndexSymbols setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
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

    public BindingId describe(@Nonnull IBinding binding) {
      if (null == binding) return null;
      final String path = getPath(binding);
      if (null == path) return null;
      if (path.contains("::lambda$")) return new BindingId(path, "Lambda");
      else return new BindingId(path, getType(binding));
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

    public String getPath(IBinding binding) {
      if (binding instanceof IVariableBinding) {
        IVariableBinding variableBinding = (IVariableBinding) binding;
        if (variableBinding.isField()) {
          final FieldBinding fieldBinding = getField(variableBinding, "binding");
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
          final IMethodBinding declaringMethod = variableBinding.getDeclaringMethod();
          return (null==declaringMethod?"null":getPath(declaringMethod)) + "::" + (null == variableBinding ? "null" : variableBinding.getName());
        }
      } else if (binding instanceof IMethodBinding) {
        IMethodBinding methodBinding = getImplementation((IMethodBinding) binding);
        final String typeBinding = null == methodBinding ? "null" : getPath(methodBinding.getDeclaringClass());
        return typeBinding + "::" + methodName(methodBinding);
      } else if (binding instanceof ITypeBinding) {
        final ITypeBinding typeBinding = (ITypeBinding) binding;
        if (typeBinding.isAnonymous()) {
          final String[] split = typeBinding.getKey().split("~");
          if(split.length < 2) return getPath(typeBinding.getDeclaringClass()) + "." + typeBinding.getKey();
          return getPath(typeBinding.getDeclaringClass()) + "." + split[1];
        } else {
          return typeBinding.getQualifiedName();
        }
      } else if (binding instanceof IPackageBinding) {
        return binding.getName();
      } else {
        final String msg = String.format("Cannot format path of %s", null==binding?"null":binding.getClass().getCanonicalName());
        logger.warn(msg);
        throw new RuntimeException(msg);
      }
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

    public String methodName(IMethodBinding methodBinding) {
      if (null == methodBinding) return "null";
      return String.format("%s(%s)",
          methodBinding.getName(),
          Arrays.stream(methodBinding.getParameterTypes()).map(x -> x.getQualifiedName()).reduce((a, b) -> a + "," + b).orElse("")
      );
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

    @Override
    public String toString() {
      return String.format("%s %s", type, path);
    }
  }
}
