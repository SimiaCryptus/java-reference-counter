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

import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@RefIgnore
class RemoveGC extends FileAstVisitor {

  RemoveGC(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull MethodInvocation node) {
    final Expression expression = node.getExpression();
    if (null == expression) return;
    final ITypeBinding typeBinding = expression.resolveTypeBinding();
    final String binaryName = typeBinding.getBinaryName();
    if (null != binaryName && binaryName.equals(System.class.getCanonicalName())) {
      node.getParent().delete();
    }
  }

}
