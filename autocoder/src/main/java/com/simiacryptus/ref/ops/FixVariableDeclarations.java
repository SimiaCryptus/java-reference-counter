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
    final Type nodeType = node.getType();
    ITypeBinding typeBinding = nodeType.resolveBinding();
    if (null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (1 != node.fragments().size()) {
      warn(node, "%s fragments", node.fragments().size());
      return;
    }
    VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.fragments().get(0);
    final Expression initializer = fragment.getInitializer();
    if (null == initializer) {
      info(node, "No initializer");
      return;
    }
    ITypeBinding initializerType = initializer.resolveTypeBinding();
    if (null == initializerType) {
      warn(node, "Unresolved binding");
      return;
    }
    if (typeBinding.isArray() != initializerType.isArray()) {
      warn(node, "Array mismatch: %s != %s", typeBinding.getQualifiedName(), initializerType.getQualifiedName());
      return;
    }
    if (initializerType.isArray()) {
      initializerType = initializerType.getElementType();
    }
    if (typeBinding.isArray()) {
      typeBinding = typeBinding.getElementType();
    }
    if (!initializerType.isAssignmentCompatible(typeBinding)) {
      final Type newType = getType(node, initializerType.getQualifiedName(), true);
      info(node, "Fixing variable type %s to %s for %s", nodeType, newType, fragment.getName());
      node.setType(newType);
    }
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    super.endVisit(node);
  }
}
