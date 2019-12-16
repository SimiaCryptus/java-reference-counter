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

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

@RefIgnore
public class InsertGCs extends FileAstVisitor {

  public InsertGCs(ProjectInfo projectInfo, CompilationUnit cu, File file) {
    super(projectInfo, cu, file);
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    final Block body = node.getBody();
    if (!node.isConstructor()) {
      body.statements().add(0, newGCCall(ast));
    }
    if (null != body && body.statements().size() > 0) {
      final ArrayList<ReturnStatement> returnStatements = new ArrayList<>();
      body.accept(new ASTVisitor() {
        @Override
        public void endVisit(ReturnStatement node) {
          final Block block = ast.newBlock();
          block.statements().add(newGCCall(ast));
          block.statements().add(copyIfAttached(node));
          replace(node, block);
          returnStatements.add(node);
        }
      });
      if (returnStatements.isEmpty()) {
        body.statements().add(newGCCall(ast));
      }
    }
  }

  @NotNull
  public ASTNode newGCCall(AST ast) {
    final MethodInvocation methodInvocation = ast.newMethodInvocation();
    methodInvocation.setExpression(ast.newSimpleName("System"));
    methodInvocation.setName(ast.newSimpleName("gc"));
    return ast.newExpressionStatement(methodInvocation);
  }
}
