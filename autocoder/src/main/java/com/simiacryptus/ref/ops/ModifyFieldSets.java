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
import org.jetbrains.annotations.NotNull;

import java.io.File;

@RefIgnore
public class ModifyFieldSets extends RefFileAstVisitor {

  public ModifyFieldSets(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull Assignment assignment) {
    final Expression leftHandSide = assignment.getLeftHandSide();
    final boolean isFieldSet = (leftHandSide instanceof FieldAccess) ||
        ((leftHandSide instanceof SimpleName) && isField((SimpleName) leftHandSide));
    final boolean isFinal;
    if(leftHandSide instanceof FieldAccess) {
      final IVariableBinding fieldBinding = resolveFieldBinding((FieldAccess) leftHandSide);
      if (null == fieldBinding) {
        warn(assignment, "Unresolved binding: %s", leftHandSide);
        return;
      }
      isFinal = 0 != (fieldBinding.getModifiers() & Modifier.FINAL);
    } else {
      isFinal = false;
    }
    if (isFieldSet) {
      final ITypeBinding typeBinding = resolveTypeBinding(leftHandSide);
      if (null == typeBinding) {
        warn(assignment, "Unresolved binding: %s", typeBinding);
        return;
      }
      if (!isRefCounted(leftHandSide, typeBinding)) return;
      final ASTNode parent = assignment.getParent();
      if (parent instanceof ExpressionStatement) {
        final ExpressionStatement expressionStatement = (ExpressionStatement) parent;
        final ASTNode parent2 = expressionStatement.getParent();
        if (parent2 instanceof Block) {
          final Block block = (Block) parent2;
          final int lineNumber = block.statements().indexOf(expressionStatement);
          final Expression rightHandSide = assignment.getRightHandSide();
          if (rightHandSide instanceof Name) {
            if (!isFinal) block.statements().add(lineNumber, freeRefStatement(leftHandSide, typeBinding));
            assignment.setRightHandSide(wrapAddRef(rightHandSide, typeBinding));
            info(assignment, "Simple field-set statement at line " + lineNumber);
          } else {
            final Block exchangeBlock = ast.newBlock();
            replace(expressionStatement, exchangeBlock);
            final String identifier = getTempIdentifier(assignment);
            exchangeBlock.statements().add(newLocalVariable(identifier, rightHandSide, getType(assignment, typeBinding.getQualifiedName(), true)));
            if (!isFinal) exchangeBlock.statements().add(freeRefStatement(leftHandSide, typeBinding));
            assignment.setRightHandSide(wrapAddRef(ast.newSimpleName(identifier), typeBinding));
            exchangeBlock.statements().add(expressionStatement);
            info(assignment, "Complex field-set statement at line " + lineNumber);
          }
        } else {
          warn(assignment, "Non-block field-set statement: %s (%s)", parent.getClass(), parent);
        }
      } else {
        warn(assignment, "Non-ExpressionStatement field-set statement: %s (%s)", parent.getClass(), parent);
      }
    }
    super.endVisit(assignment);
  }

}
