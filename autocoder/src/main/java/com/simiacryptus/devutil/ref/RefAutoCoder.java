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

package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.AutoCoder;
import com.simiacryptus.devutil.ops.IndexSymbols;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class RefAutoCoder extends AutoCoder {

  private boolean verbose = true;
  private boolean addRefcounting = true;

  public RefAutoCoder(@NotNull String pathname) {
    super(pathname);
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
//    if (isVerbose()) rewrite((cu, file) -> new LogNodes(cu, file));
    rewrite((cu, file) -> new RemoveRefs(cu, file));
    rewrite((cu, file) -> new ReplaceTypes(cu, file, true));
    while (rewrite((cu, file) -> new InlineRefs(cu, file)) > 0) {
      logger.info("Re-running InlineRefs");
    }
    while (rewrite((cu, file) -> new InlineTempVars(cu, file)) > 0) {
      logger.info("Re-running InlineRefs");
    }
    if (isAddRefcounting()) {
      rewrite((cu, file) -> new ReplaceTypes(cu, file, false));
      rewrite((cu, file) -> new InsertMethods(cu, file));
      rewrite((cu, file) -> new InsertAddRefs(cu, file));
      rewrite((cu, file) -> new ModifyFieldSets(cu, file));
      rewrite((cu, file) -> new InsertFreeRefs(cu, file));
      final IndexSymbols.SymbolIndex index = getSymbolIndex();
      rewrite((cu, file) -> new InstrumentClosures(cu, file, index));
      // Test and fix complex function branching; return and throw and if, while, and for
      // Optimize adjacent addRef / freeRef
      // Identify Entry<?,?> objects and free
      // addRef and freeRef for pure interfaces (just in case)
    }
  }

}
