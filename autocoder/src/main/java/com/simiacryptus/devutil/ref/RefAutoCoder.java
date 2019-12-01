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
import com.simiacryptus.devutil.ops.LogNodes;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class RefAutoCoder extends AutoCoder {

  private boolean verbose = false;
  private boolean addRefcounting = true;

  public RefAutoCoder(@NotNull String pathname) {
    super(pathname);
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

  public boolean isRefAware(ITypeBinding declaringClass) {
    return isRefAware(declaringClass.getPackage());
  }

  public boolean isRefAware(IMethodBinding methodBinding) {
    return isRefAware(methodBinding.getDeclaringClass());
  }

  public boolean isRefAware(IPackageBinding classPackage) {
    return toString(classPackage).startsWith("com.simiacryptus");
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
    if (isVerbose()) rewrite((cu, file) -> new LogNodes(cu, file));
    rewrite((cu, file) -> new RemoveRefs(this, cu, file));
    rewrite((cu, file) -> new ReplaceTypes(this, cu, file, true));
    while (rewrite((cu, file) -> new InlineRefs(this, cu, file)) > 0) {
      logger.info("Re-running InlineRefs");
    }
    while (rewrite((cu, file) -> new InlineTempVars(this, cu, file)) > 0) {
      logger.info("Re-running InlineRefs");
    }
    if (isAddRefcounting()) {
      rewrite((cu, file) -> new ReplaceTypes(this, cu, file, false));
      rewrite((cu, file) -> new InsertMethods(this, cu, file));
      rewrite((cu, file) -> new InsertAddRefs(this, cu, file));
      rewrite((cu, file) -> new ModifyFieldSets(this, cu, file));
      rewrite((cu, file) -> new InsertFreeRefs(this, cu, file));
      IndexSymbols.SymbolIndex index = getSymbolIndex();
      rewrite((cu, file) -> new InstrumentClosures(this, cu, file, index));
      // TODO: Mark ref-aware classes with annotations and use them to identify consumers
      // TODO: Optimize adjacent addRef / freeRef
      // TODO: Package as maven plugin
    }
  }

}
