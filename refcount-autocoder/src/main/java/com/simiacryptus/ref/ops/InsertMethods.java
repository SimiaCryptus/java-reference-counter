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
import com.simiacryptus.ref.core.ops.ASTOperator;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RefIgnore
public class InsertMethods extends RefASTOperator {

  protected InsertMethods(ProjectInfo projectInfo, @Nonnull CompilationUnit cu, @Nonnull File file) {
    super(projectInfo, cu, file);
  }

  @Nonnull
  public MethodDeclaration method_addRef(@Nonnull ASTNode node, @Nonnull SimpleName name, boolean isInterface, TypeParameter... typeParameters) {
    final String fqTypeName = name.getFullyQualifiedName();
    final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
    methodDeclaration.setName(ast.newSimpleName("addRef"));
    methodDeclaration.setReturnType2(getType(node, fqTypeName, typeParameters));
    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
    if (!isInterface) {
      methodDeclaration.modifiers().add(ASTUtil.annotation_override(ast));
      methodDeclaration.modifiers().add(ASTUtil.annotation_SuppressWarnings(ast, "unused"));
      final Block block = ast.newBlock();
      final CastExpression castExpression = ast.newCastExpression();
      castExpression.setType(getType(node, fqTypeName, typeParameters));
      final SuperMethodInvocation superMethodInvocation = ast.newSuperMethodInvocation();
      superMethodInvocation.setName(ast.newSimpleName("addRef"));
      castExpression.setExpression(superMethodInvocation);
      final ReturnStatement returnStatement = ast.newReturnStatement();
      returnStatement.setExpression(castExpression);
      block.statements().add(returnStatement);
      methodDeclaration.setBody(block);
    }
    return methodDeclaration;
  }

  @Nonnull
  public MethodDeclaration method_free(@Nonnull AST ast, boolean isAbstract, boolean isOverride) {
    final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
    methodDeclaration.setName(ast.newSimpleName("_free"));
    methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
    if (!isAbstract) {
      if (isOverride) methodDeclaration.modifiers().add(ASTUtil.annotation_override(ast));
      methodDeclaration.modifiers().add(ASTUtil.annotation_SuppressWarnings(ast, "unused"));
      final Block body = ast.newBlock();
      if (isOverride) {
        final SuperMethodInvocation superCall = ast.newSuperMethodInvocation();
        superCall.setName(ast.newSimpleName("_free"));
        body.statements().add(ast.newExpressionStatement(superCall));
      }
      methodDeclaration.setBody(body);
    }
    return methodDeclaration;
  }

  @Nonnull
  private Type getType(@Nonnull ASTNode node, @Nonnull String fqTypeName, @Nonnull TypeParameter... typeParameters) {
    final Type baseType = getType(node, fqTypeName, false);
    if (typeParameters.length > 0) {
      final ParameterizedType parameterizedType = ast.newParameterizedType(baseType);
      for (TypeParameter typeParameter : typeParameters) {
        final ITypeBinding binding = resolveBinding(typeParameter);
        if (binding == null) {
          warn(typeParameter, "Unresolved Binding %s", typeParameter);
          parameterizedType.typeArguments().add(ast.newWildcardType());
          continue;
        }
        parameterizedType.typeArguments().add(getType(typeParameter, binding.getQualifiedName(), false));
      }
      return parameterizedType;
    } else {
      assert baseType != null;
      return baseType;
    }
  }

  @RefIgnore
  public static class ModifyTypeDeclaration extends InsertMethods {
    public ModifyTypeDeclaration(ProjectInfo projectInfo, @Nonnull CompilationUnit cu, @Nonnull File file) {
      super(projectInfo, cu, file);
    }

    @Override
    public void endVisit(@Nonnull TypeDeclaration node) {
      final ITypeBinding typeBinding = ASTOperator.resolveBinding(node);
      if (null == typeBinding) {
        warn(node, "Unresolved binding");
        return;
      }
      if (ASTUtil.derives(typeBinding, ReferenceCounting.class)) {
        final boolean isInterface = node.isInterface();
        final boolean isOverride = !ASTUtil.derives(typeBinding, ReferenceCountingBase.class);
        final List declarations = node.bodyDeclarations();
        final Optional<MethodDeclaration> freeMethod = ASTUtil.findMethod(node, "_free");
        if (!freeMethod.isPresent()) {
          declarations.add(method_free(ast, isInterface, isOverride));
        } else {
          final MethodDeclaration methodDeclaration = RefUtil.get(freeMethod);
          final int modifiers = methodDeclaration.getModifiers();
          if (0 == (modifiers & Modifier.PUBLIC)) {
            methodDeclaration.modifiers().clear();
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
            if (0 != (modifiers & Modifier.ABSTRACT)) {
              methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD));
            }
          }
        }
        final TypeParameter[] typeParameters = ((Stream<TypeParameter>) node.typeParameters().stream()).toArray(i -> new TypeParameter[i]);
        declarations.add(method_addRef(node, node.getName(), isInterface, typeParameters));
        //declarations.add(method_freeRefs(ast, node.getName()));
      }
    }
  }

  @RefIgnore
  public static class ModifyAnonymousClassDeclaration extends InsertMethods {
    public ModifyAnonymousClassDeclaration(ProjectInfo projectInfo, @Nonnull CompilationUnit cu, @Nonnull File file) {
      super(projectInfo, cu, file);
    }

    @Override
    public void endVisit(@Nonnull AnonymousClassDeclaration node) {
      final ITypeBinding typeBinding = resolveBinding(node);
      if (null == typeBinding) {
        warn(node, "Unresolved binding");
        return;
      }
      if (ASTUtil.derives(typeBinding, ReferenceCounting.class)) {
        final List declarations = node.bodyDeclarations();
        final Optional<MethodDeclaration> freeMethod = ASTUtil.findMethod(node, "_free");
        if (!freeMethod.isPresent()) {
          declarations.add(method_free(ast, false, !ASTUtil.derives(typeBinding, ReferenceCountingBase.class)));
        } else {
          final MethodDeclaration methodDeclaration = RefUtil.get(freeMethod);
          final int modifiers = methodDeclaration.getModifiers();
          if (0 == (modifiers & Modifier.PUBLIC)) {
            methodDeclaration.modifiers().clear();
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
            if (0 != (modifiers & Modifier.ABSTRACT)) {
              methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD));
            }
          }
        }
      }
    }
  }

}
