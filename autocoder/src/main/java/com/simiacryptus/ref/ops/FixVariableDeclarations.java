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

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.List;

@RefIgnore
public class FixVariableDeclarations extends RefFileAstVisitor {

  public FixVariableDeclarations(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  private Type apply(Type type, Expression initializer) {
    final ITypeBinding typeBinding = resolveBinding(type);
    if (null == typeBinding) {
      warn(type, "Unresolved binding for %s", type);
      return null;
    }
    if (null == initializer) {
      info(type, "No initializer");
      return null;
    }
    final ITypeBinding initializerType = resolveTypeBinding(initializer);
    if (null == initializerType) {
      warn(type, "Unresolved binding");
      return null;
    }
    if (initializerType.isAssignmentCompatible(typeBinding)) {
      return null;
    }
    Type newType = commonSuperclass(type, typeBinding, initializerType);
    warn(type, "Replaced variable type %s to %s", type, newType);
    return newType;
  }

  private Type commonInterface(Type node, ITypeBinding typeBinding, ITypeBinding initializerType) {
    if (initializerType.isAssignmentCompatible(typeBinding)) {
      return getType(node, typeBinding.getQualifiedName(), true);
    }
    for (ITypeBinding interfaceBinding : initializerType.getInterfaces()) {
      final Type commonInterface = commonInterface(node, interfaceBinding, initializerType);
      if (null != commonInterface) return commonInterface;
    }
    return null;
  }

  private Type commonSuperclass(Type node, ITypeBinding typeBinding, ITypeBinding initializerType) {
    if (initializerType.isAssignmentCompatible(typeBinding)) {
      return getType(node, typeBinding.getQualifiedName(), true);
    }
    final ITypeBinding superclass = initializerType.getSuperclass();
    if (null != superclass && !superclass.getQualifiedName().equals(Object.class)) {
      final Type commonSuperclass = commonSuperclass(node, superclass, initializerType);
      if (null != commonSuperclass) return commonSuperclass;
    }
    return commonInterface(node, superclass, initializerType);
  }

  @Override
  public void endVisit(VariableDeclarationStatement node) {
    final Type type = node.getType();
    final List fragments = node.fragments();
    if (1 != fragments.size()) {
      warn(node, "%s fragments", fragments.size());
      return;
    }
    final Type newType = apply(type, ((VariableDeclarationFragment) fragments.get(0)).getInitializer());
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
    final Type newType = apply(type, ((VariableDeclarationFragment) fragments.get(0)).getInitializer());
    if (null != newType && type != newType) node.setType(newType);
  }

  @Override
  public void endVisit(VariableDeclarationFragment node) {
    super.endVisit(node);
  }
}
