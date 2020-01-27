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

package com.simiacryptus.ref.ops;

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import com.simiacryptus.ref.core.Tuple2;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;

@RefIgnore
public class VerifyAssignments extends RefASTOperator {

  private @Nonnull
  SymbolIndex symbolIndex;

  public VerifyAssignments(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this(projectInfo, compilationUnit, file, new SymbolIndex());
    getSymbolIndex(compilationUnit, symbolIndex);
  }

  public VerifyAssignments(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, @Nonnull SymbolIndex symbolIndex) {
    super(projectInfo, compilationUnit, file);
    this.symbolIndex = symbolIndex;
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    Expression initializer = node.getInitializer();
    if (null != initializer && isRefAware(initializer) && !isRefAware(node)) {
      fatal(node, "Assignment loses reference awareness");
    }
  }

  @Override
  public void endVisit(@Nonnull Assignment node) {
    if (isRefAware(node.getRightHandSide()) && !isRefAware(node.getLeftHandSide())) {
      fatal(node, "Assignment loses reference awareness");
    }
  }

  @Override
  public void endVisit(@Nonnull ReturnStatement node) {
    final Expression expression = node.getExpression();
    if (null == expression) return;
    if (isRefAware(expression)) {
      final Tuple2<ASTNode, IMethodBinding> method = ASTUtil.getMethodTuplet(node);
      if (method == null) {
        fatal(node, "No containing method");
        return;
      }
      IMethodBinding methodBinding = method._2;
      if (methodBinding == null) {
        warn(node, "Unresolved method");
        return;
      }
      final ITypeBinding returnType = methodBinding.getReturnType();
      if (!isRefCounted(node, returnType) && !ASTUtil.hasAnnotation(methodBinding, RefAware.class) && !returnType.isGenericType()) {
        fatal(node, "Return of %s loses RefCount typing in %s", expression.resolveTypeBinding().getQualifiedName(), returnType.getQualifiedName());
      }
    }
  }

  private boolean isRefAware(@Nonnull VariableDeclarationFragment fragment) {
    IVariableBinding typeBinding = fragment.resolveBinding();
    if (null == typeBinding) {
      warn(fragment, "Unresolved binding");
      return false;
    }
    if (typeBinding.getType().isPrimitive()) {
      debug(fragment, "Primitive type");
      return false;
    }
    IVariableBinding fragmentBinding = fragment.resolveBinding();
    if (null == fragmentBinding) {
      warn(fragment, "Unresolved binding");
      return false;
    }
    return isRefCounted(fragment, fragmentBinding.getType()) || ASTUtil.hasAnnotation(fragmentBinding, RefAware.class);
  }

  private boolean isRefAware(@Nonnull Expression expression) {
    if(expression instanceof FieldAccess) {
      IVariableBinding fieldBinding = ((FieldAccess) expression).resolveFieldBinding();
      if(null != fieldBinding) {
        if(ASTUtil.hasAnnotation(fieldBinding, RefAware.class)) return true;
      }
    }
    ITypeBinding typeBinding = expression.resolveTypeBinding();
    if (null == typeBinding) {
      warn(expression, "Unresolved binding");
      return false;
    }
    if (typeBinding.isPrimitive()) {
      debug(expression, "Primitive type");
      return false;
    }
    SymbolIndex.BindingID bindingID;
    if (expression instanceof Name) {
      IBinding binding = ((Name) expression).resolveBinding();
      if (null == binding) {
        warn(expression, "Unresolved binding");
        return false;
      }
      bindingID = SymbolIndex.getBindingID(binding);
    } else {
      bindingID = null;
    }
    if (null != bindingID) {
      ASTNode definition = symbolIndex.definitions.get(bindingID);
      if (null == definition) {
        info(expression, "Definition not found: %s", bindingID);
        return false;
      }
      if (definition instanceof VariableDeclarationFragment) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) definition;
        IVariableBinding fragmentBinding = fragment.resolveBinding();
        if (null == fragmentBinding) {
          warn(fragment, "Unresolved binding");
          return false;
        }
        return isRefCounted(expression, fragmentBinding.getType()) || ASTUtil.hasAnnotation(fragmentBinding, RefAware.class);
      } else if (definition instanceof FieldDeclaration) {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) definition;
        List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        if (fragments.size() > 1) fatal(fieldDeclaration, "Multiple defined fields");
        IVariableBinding fragmentBinding = fragments.get(0).resolveBinding();
        if (null == fragmentBinding) {
          warn(fieldDeclaration, "Unresolved binding");
          return false;
        }
        return isRefCounted(expression, fragmentBinding.getType()) || ASTUtil.hasAnnotation(fragmentBinding, RefAware.class);
      } else if (definition instanceof SingleVariableDeclaration) {
        SingleVariableDeclaration fragment = (SingleVariableDeclaration) definition;
        IVariableBinding fragmentBinding = fragment.resolveBinding();
        if (null == fragmentBinding) {
          warn(fragment, "Unresolved binding");
          return false;
        }
        return isRefCounted(expression, fragmentBinding.getType()) || ASTUtil.hasAnnotation(fragmentBinding, RefAware.class);
      } else {
        warn(definition, "Unhandled declaration node type: %s", definition.getClass().getSimpleName());
        return false;
      }
    } else {
      return isRefCounted(expression, typeBinding);
    }
  }
}
