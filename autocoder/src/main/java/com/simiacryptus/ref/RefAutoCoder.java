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

package com.simiacryptus.ref;

import com.simiacryptus.ref.core.AutoCoder;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.ops.IndexSymbols;
import com.simiacryptus.ref.core.ops.LogNodes;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.ops.*;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;

@RefIgnore
public class RefAutoCoder extends AutoCoder {

  private boolean verbose = false;
  private boolean addRefcounting = true;

  public RefAutoCoder(ProjectInfo projectInfo) {
    super(projectInfo);
  }

  @NotNull
  public static String toString(IPackageBinding declaringClassPackage) {
    return Arrays.stream(declaringClassPackage.getNameComponents()).reduce((a, b) -> a + "." + b).get();
  }

  public boolean isAddRefcounting() {
    return addRefcounting;
  }

  @NotNull
  public RefAutoCoder setAddRefcounting(boolean addRefcounting) {
    this.addRefcounting = addRefcounting;
    return this;
  }

  public boolean isVerbose() {
    return verbose;
  }

  @NotNull
  public RefAutoCoder setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  @Override
  @Nonnull
  public void rewrite() {
    if (isVerbose()) rewrite(LogNodes::new);
    rewrite(RemoveAnnotations::new);
    while (rewrite(RemoveRefs::new) > 0) {
      logger.info("Re-running RemoveRefs");
    }
    rewrite((projectInfo, cu, file) -> new ReplaceTypes(projectInfo, cu, file, true));
    rewrite(DistinctImports::new);
    rewrite(FixVariableDeclarations::new);
    while (rewrite(InlineRefs::new) > 0) {
      logger.info("Re-running InlineRefs");
    }
    while (rewrite(InlineTempVars::new) > 0) {
      logger.info("Re-running InlineRefs");
    }
    if (isAddRefcounting()) {
      rewrite(InsertAnnotations::new);
      rewrite((projectInfo, cu, file) -> new ReplaceTypes(projectInfo, cu, file, false));
      rewrite(DistinctImports::new);
      rewrite(FixCustomImplementations::new);
      rewrite(FixVariableDeclarations::new);
      rewrite(InsertMethods::new);
      rewrite(InsertAddRefs::new);
      rewrite(ModifyFieldSets::new);
      rewrite(InsertFreeRefs::new);
      IndexSymbols.SymbolIndex index = getSymbolIndex();
      rewrite((projectInfo, cu, file) -> new InstrumentClosures(projectInfo, cu, file, index));
      rewrite(OptimizeRefs::new);
    }
  }

}
