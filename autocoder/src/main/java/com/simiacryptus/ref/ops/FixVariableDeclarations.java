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

package com.simiacryptus.ref.ops;

import com.simiacryptus.ref.lang.RefCoderIgnore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;

@RefCoderIgnore
public class FixVariableDeclarations extends RefFileAstVisitor {

  public FixVariableDeclarations(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(VariableDeclarationStatement node) {
    final Type type = node.getType();
    final ITypeBinding typeBinding = type.resolveBinding();
    if(null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if(1 != node.fragments().size()) {
      warn(node, "%s fragments", node.fragments().size());
      return;
    }
    VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.fragments().get(0);
    final Expression initializer = fragment.getInitializer();
    final ITypeBinding initializerType = initializer.resolveTypeBinding();
    if(null == initializerType) {
      warn(node, "Unresolved binding");
      return;
    }

    if(!initializerType.isAssignmentCompatible(typeBinding)) {
      final Type newType = getType(initializer);
      warn(node, "Fixing variable type %s to %s", node.getType(), newType);
      node.setType(newType);
    }
    super.endVisit(node);
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    super.endVisit(node);
  }
}