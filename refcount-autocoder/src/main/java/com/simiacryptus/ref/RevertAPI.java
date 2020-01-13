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

package com.simiacryptus.ref;

import com.simiacryptus.ref.core.AutoCoder;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.ops.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@RefIgnore
@Mojo(name = "revertAPI")
public class RevertAPI extends RefAutoCoderMojo {
  @NotNull
  @Override
  protected AutoCoder getAutoCoder(ProjectInfo projectInfo) {
    return new Coder(projectInfo);
  }

  public static class Coder extends AutoCoder {
    public Coder(ProjectInfo projectInfo) {
      super(projectInfo);
    }

    @Override
    @Nonnull
    public void rewrite() {
      rewrite((projectInfo, cu, file) -> new ReplaceTypes.ModifyTypeParameter(projectInfo, cu, file, true));
      rewrite((projectInfo, cu, file) -> new ReplaceTypes.ModifySimpleName(projectInfo, cu, file, true));
      rewrite((projectInfo, cu, file) -> new ReplaceTypes.ModifyQualifiedName(projectInfo, cu, file, true));
      rewrite((projectInfo, cu, file) -> new ReplaceTypes.ModifyCompilationUnit(projectInfo, cu, file, true));
      rewrite(DistinctImports::new);
      rewrite(FixVariableDeclarations.ModifyFieldDeclaration::new);
      rewrite(FixVariableDeclarations.ModifyVariableDeclarationFragment::new);
      rewrite(FixVariableDeclarations.ModifyVariableDeclarationStatement::new);
      rewrite(RemoveRefMethods::new);
    }
  }
}
