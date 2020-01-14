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
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VisitClosures extends RefASTOperator {
  @Nonnull
  protected final SymbolIndex index;

  public VisitClosures(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
    this.index = getSymbolIndex(compilationUnit);
  }

  public static Collection<SymbolIndex.BindingID> getClosures(@Nonnull RefASTOperator visitClosures, @Nonnull SymbolIndex index, @Nonnull ASTNode node) {
    return visitClosures.getSymbolIndex(node).references.entrySet().stream().flatMap(e -> {
      final SymbolIndex.BindingID bindingID = e.getKey();
      if (!bindingID.type.equals("Type")) {
        final ASTNode definition = index.definitions.get(bindingID);
        if (definition == null) {
          visitClosures.info(node, "Unresolved definition for %s in %s", bindingID, visitClosures.getSpan(node));
        } else {
          final List<ASTNode> references = e.getValue();
          final String locationReport = RefUtil.get(references.stream()
              .map(x -> visitClosures.getSpan(x).toString())
              .reduce((a, b) -> a + ", " + b));
          if (!ASTUtil.contains(node, definition)) {
            visitClosures.debug(definition, String.format("Closure %s referenced at %s defined by %s", bindingID, locationReport, visitClosures.getSpan(node)));
            if (!references.isEmpty()) return Stream.of(bindingID);
          } else {
            visitClosures.debug(definition, String.format("In-scope symbol %s referenced at %s defined by %s", bindingID, locationReport, visitClosures.getSpan(definition)));
          }
        }
      }
      return Stream.empty();
    }).collect(Collectors.toList());
  }

  protected Collection<SymbolIndex.BindingID> getClosures(@Nonnull ASTNode node) {
    return getClosures(this, index, node);
  }
}
