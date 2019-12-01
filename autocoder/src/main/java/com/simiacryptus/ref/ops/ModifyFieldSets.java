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

import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ModifyFieldSets extends RefFileAstVisitor {

  public ModifyFieldSets(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull Assignment node) {
    if (node.getLeftHandSide() instanceof FieldAccess) {
      final FieldAccess fieldAccess = (FieldAccess) node.getLeftHandSide();
      final ITypeBinding typeBinding = fieldAccess.resolveTypeBinding();
      if (null == typeBinding) {
        warn(node, "Unresolved binding");
        return;
      }
      if (!isRefCounted(fieldAccess, typeBinding)) return;
      final ASTNode parent = node.getParent();
      if (parent instanceof ExpressionStatement) {
        final ASTNode parent2 = parent.getParent();
        if (parent2 instanceof Block) {
          final Block block = (Block) parent2;
          final int lineNumber = block.statements().indexOf(parent);
          final Expression rightHandSide = node.getRightHandSide();
          final AST ast = node.getAST();
          if (rightHandSide instanceof Name) {
            block.statements().add(lineNumber, freeRefStatement(fieldAccess, fieldAccess.resolveTypeBinding()));
            node.setRightHandSide(wrapAddRef(rightHandSide, typeBinding));
            info(node, "Simple field-set statement at line " + lineNumber);
          } else {
            final Block exchangeBlock = ast.newBlock();
            final String identifier = randomIdentifier(node);
            exchangeBlock.statements().add(newLocalVariable(identifier, rightHandSide, getType(node, typeBinding.getQualifiedName())));
            exchangeBlock.statements().add(freeRefStatement(fieldAccess, fieldAccess.resolveTypeBinding()));
            final Assignment assignment = ast.newAssignment();
            assignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, fieldAccess));
            assignment.setOperator(Assignment.Operator.ASSIGN);
            assignment.setRightHandSide(wrapAddRef(ast.newSimpleName(identifier), typeBinding));
            exchangeBlock.statements().add(ast.newExpressionStatement(assignment));
            block.statements().set(lineNumber, exchangeBlock);
            info(node, "Complex field-set statement at line " + lineNumber);
          }
        } else {
          warn(node, "Non-block field-set statement: %s (%s)", parent.getClass(), parent);
        }
      } else {
        warn(node, "Non-ExpressionStatement field-set statement: %s (%s)", parent.getClass(), parent);
      }
    }
    super.endVisit(node);
  }

}
