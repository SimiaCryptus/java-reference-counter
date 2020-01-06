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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RefIgnore
public class VerifyMethodCalls extends RefASTOperator {

  final HashMap<SymbolIndex.BindingID, Set<Integer>> missingAttributes;

  public VerifyMethodCalls(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    this(projectInfo, compilationUnit, file, null);
  }

  public VerifyMethodCalls(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, HashMap<SymbolIndex.BindingID, Set<Integer>> missingAttributes) {
    super(projectInfo, compilationUnit, file);
    this.missingAttributes = missingAttributes;
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    final IMethodBinding methodBinding = node.resolveBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    final List<IMethodBinding> superMethods = ASTUtil.superMethods(methodBinding);
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      if(!ASTUtil.findAnnotation(RefAware.class, methodBinding.getParameterAnnotations(i)).isPresent()) {
        for (IMethodBinding superMethod : superMethods) {
          if (ASTUtil.findAnnotation(RefAware.class, superMethod.getParameterAnnotations(i)).isPresent()) {
            fail(node, methodBinding, i);
            break;
          }
        }
      }
    }
  }

  @Override
  public void endVisit(MethodInvocation node) {
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    final List<Expression> arguments = node.arguments();
    final int numberOfDeclaredArguments = methodBinding.getParameterTypes().length;
    final int numberOfArguments = arguments.size();
    for (int i = 0; i < numberOfArguments; i++) {
      final Expression argument = arguments.get(i);
      final ITypeBinding parameterType;
      final IAnnotationBinding[] parameterAnnotations;
      if(numberOfArguments > numberOfDeclaredArguments && i >= numberOfDeclaredArguments) {
        parameterType = methodBinding.getParameterTypes()[numberOfDeclaredArguments-1].getElementType();
        parameterAnnotations = methodBinding.getParameterAnnotations(numberOfDeclaredArguments-1);
      } else {
        parameterType = methodBinding.getParameterTypes()[i];
        parameterAnnotations = methodBinding.getParameterAnnotations(i);
      }
      final ITypeBinding resolveTypeBinding = argument.resolveTypeBinding();
      if (null == resolveTypeBinding) {
        warn(argument, "Cannot resolve type binding");
        continue;
      }
      if (isRefCounted(argument, resolveTypeBinding)) {
        if (!isRefCounted(argument, parameterType) && !ASTUtil.findAnnotation(RefAware.class, parameterAnnotations).isPresent()) {
          fail(argument, methodBinding, i);
        }
      }
    }
  }

  private void fail(ASTNode node, IMethodBinding methodBinding, int i) {
    if (null == missingAttributes) {
      fatal(node, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    } else {
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(methodBinding);
      missingAttributes.computeIfAbsent(bindingID, x -> new HashSet<>()).add(i);
      warn(node, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    }
  }
}
