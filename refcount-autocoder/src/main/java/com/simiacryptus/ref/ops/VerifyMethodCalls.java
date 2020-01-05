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
  private @NotNull SymbolIndex symbolIndex;

  public VerifyMethodCalls(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    this(projectInfo, compilationUnit, file, null);
  }

  public VerifyMethodCalls(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, HashMap<SymbolIndex.BindingID, Set<Integer>> missingAttributes) {
    super(projectInfo, compilationUnit, file);
    symbolIndex = getSymbolIndex(compilationUnit);
    this.missingAttributes = missingAttributes;
  }

  @Override
  public void endVisit(MethodInvocation node) {
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    final List<Expression> arguments = node.arguments();
    for (int i = 0; i < arguments.size(); i++) {
      final Expression argument = arguments.get(i);
      final ITypeBinding resolveTypeBinding = argument.resolveTypeBinding();
      if (null == resolveTypeBinding) {
        warn(argument, "Cannot resolve type binding");
        continue;
      }
      if (isRefCounted(argument, resolveTypeBinding)) {
        if (!isRefCounted(argument, methodBinding.getParameterTypes()[i]) || !ASTUtil.hasAnnotation(RefAware.class, methodBinding.getParameterAnnotations(i))) {
          fail(methodBinding, i, argument);
        }
      }
    }
  }

  private void fail(IMethodBinding methodBinding, int i, Expression argument) {
    if (null == missingAttributes) {
      fatal(argument, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    } else {
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(methodBinding);
      missingAttributes.computeIfAbsent(bindingID, x -> new HashSet<>()).add(i);
      warn(argument, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    }
  }
}
