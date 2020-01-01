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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The type Fix variable declarations.
 */
@RefIgnore
public class FixVariableDeclarations extends RefASTOperator {

  /**
   * Instantiates a new Fix variable declarations.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   */
  protected FixVariableDeclarations(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * Apply type.
   *
   * @param type        the type
   * @param initializer the initializer
   * @return the type
   */
  @Nullable
  protected Type apply(@NotNull Type type, @Nullable Expression initializer) {
    final ITypeBinding typeBinding = resolveBinding(type);
    if (null == typeBinding) {
      warn(type, "Unresolved binding for %s", type);
      return null;
    }
    if (null == initializer) {
      debug(type, "No initializer");
      return null;
    }
    if (initializer instanceof NullLiteral) {
      debug(type, "Null initializer");
      return null;
    }
    final ITypeBinding initializerType = resolveTypeBinding(initializer);
    if (null == initializerType) {
      warn(type, "Unresolved binding");
      return null;
    }
    final boolean initRefMarked = ASTUtil.derives(initializerType, ReferenceCounting.class);
    if (initializerType.isAssignmentCompatible(typeBinding) && ASTUtil.derives(typeBinding, ReferenceCounting.class) == initRefMarked) {
      return null;
    }
    Optional<ITypeBinding> chooseType = Optional.empty();
    List<ITypeBinding> typePath = typePath(initializerType);
    for (int i = typePath.size() - 1; i >= 0; i--) {
      ITypeBinding candidateBinding = typePath.get(i);
      if (candidateBinding.isAssignmentCompatible(typeBinding)
          && ASTUtil.derives(candidateBinding, ReferenceCounting.class) == initRefMarked) {
        chooseType = Optional.of(candidateBinding);
        break;
      }
    }
    if (chooseType.isPresent()) {
      Type newType = getType(type, chooseType.get().getQualifiedName(), true);
      warn(type, "Replaced variable type %s to %s", type, newType);
      return newType;
    } else {
      warn(type, "No candidates fit for %s", type);
      return null;
    }
  }

  /**
   * Common interface type.
   *
   * @param node            the node
   * @param typeBinding     the type binding
   * @param initializerType the initializer type
   * @return the type
   */
  @Nullable
  protected Type commonInterface(Type node, @NotNull ITypeBinding typeBinding, @NotNull ITypeBinding initializerType) {
    if (initializerType.isAssignmentCompatible(typeBinding)) {
      return getType(node, typeBinding.getQualifiedName(), true);
    }
    for (ITypeBinding interfaceBinding : initializerType.getInterfaces()) {
      final Type commonInterface = commonInterface(node, interfaceBinding, initializerType);
      if (null != commonInterface) return commonInterface;
    }
    return null;
  }

  /**
   * Type path list.
   *
   * @param typeBinding the type binding
   * @return the list
   */
  protected List<ITypeBinding> typePath(@NotNull ITypeBinding typeBinding) {
    final ArrayList<ITypeBinding> list = new ArrayList<>();
    list.add(typeBinding);
    final ITypeBinding superclass = typeBinding.getSuperclass();
    if (null != superclass) {
      list.addAll(typePath(superclass));
    }
    for (ITypeBinding xface : typeBinding.getInterfaces()) {
      typePath(xface).forEach(list::add);
    }
    return list.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Common superclass type.
   *
   * @param node            the node
   * @param typeBinding     the type binding
   * @param initializerType the initializer type
   * @return the type
   */
  @Nullable
  @SuppressWarnings("unused")
  protected Type commonSuperclass(Type node, @NotNull ITypeBinding typeBinding, @NotNull ITypeBinding initializerType) {
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

  /**
   * The type Modify variable declaration statement.
   */
  @RefIgnore
  public static class ModifyVariableDeclarationStatement extends FixVariableDeclarations {
    /**
     * Instantiates a new Modify variable declaration statement.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
    public ModifyVariableDeclarationStatement(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull VariableDeclarationStatement node) {
      final Type type = node.getType();
      final List fragments = node.fragments();
      if (1 != fragments.size()) {
        warn(node, "%s fragments", fragments.size());
        return;
      }
      final Type newType = apply(type, ((VariableDeclarationFragment) fragments.get(0)).getInitializer());
      if (null != newType && type != newType) node.setType(newType);
    }
  }

  /**
   * The type Modify field declaration.
   */
  @RefIgnore
  public static class ModifyFieldDeclaration extends FixVariableDeclarations {
    /**
     * Instantiates a new Modify field declaration.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
    public ModifyFieldDeclaration(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull FieldDeclaration node) {
      final Type type = node.getType();
      final List fragments = node.fragments();
      if (1 != fragments.size()) {
        warn(node, "%s fragments", fragments.size());
        return;
      }
      final Type newType = apply(type, ((VariableDeclarationFragment) fragments.get(0)).getInitializer());
      if (null != newType && type != newType) node.setType(newType);
    }
  }

  /**
   * The type Modify variable declaration fragment.
   */
  @RefIgnore
  public static class ModifyVariableDeclarationFragment extends FixVariableDeclarations {
    /**
     * Instantiates a new Modify variable declaration fragment.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     */
    public ModifyVariableDeclarationFragment(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(VariableDeclarationFragment node) {
      super.endVisit(node);
    }
  }
}