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

import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.List;

@RefIgnore
public class NarrowVariableDeclarations extends RefFileAstVisitor {

  public NarrowVariableDeclarations(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  private Type apply(ASTNode node, Type type, Expression initializer) {
    final ITypeBinding typeBinding = type.resolveBinding();
    if (null == typeBinding) {
      warn(node, "Unresolved binding for %s", type);
      return null;
    }
    if (null == initializer) {
      info(node, "No initializer");
      return null;
    }
    final ITypeBinding initializerType = initializer.resolveTypeBinding();
    if (null == initializerType) {
      warn(node, "Unresolved binding");
      return null;
    }
    if (isRefAware(typeBinding) || !isRefAware(initializerType)) {
      return null;
    }
    Type newType = getType(initializer, true);
    warn(node, "Replaced variable type %s to %s", type, newType);
    return newType;
  }

  @Override
  public void endVisit(VariableDeclarationStatement node) {
    final Type type = node.getType();
    final List fragments = node.fragments();
    if (1 != fragments.size()) {
      warn(node, "%s fragments", fragments.size());
      return;
    }
    final Type newType = apply(node, type, ((VariableDeclarationFragment) fragments.get(0)).getInitializer());
    if (null != newType && type != newType) node.setType(newType);
  }

  @Override
  public void endVisit(FieldDeclaration node) {
    final Type type = node.getType();
    final List fragments = node.fragments();
    if (1 != fragments.size()) {
      warn(node, "%s fragments", fragments.size());
      return;
    }
    final Type newType = apply(node, type, ((VariableDeclarationFragment) fragments.get(0)).getInitializer());
    if (null != newType && type != newType) node.setType(newType);
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    super.endVisit(node);
  }
}
