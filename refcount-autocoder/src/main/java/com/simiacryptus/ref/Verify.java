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
import com.simiacryptus.ref.core.CollectableException;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import com.simiacryptus.ref.core.ops.IndexSymbols;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.ops.*;
import org.apache.maven.plugins.annotations.Mojo;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

@RefIgnore
@Mojo(name = "verify")
public class Verify extends RefAutoCoderMojo {
  @Nonnull
  @Override
  protected AutoCoder getAutoCoder(ProjectInfo projectInfo) {
    return new Coder(projectInfo);
  }

  @RefIgnore
  public static class Coder extends AutoCoder {

    public Coder(ProjectInfo projectInfo) {
      super(projectInfo);
    }

    @Override
    @Nonnull
    public void rewrite() {
      SymbolIndex index = new SymbolIndex();
      ArrayList<CollectableException> exceptions = new ArrayList<>();
      for (VisitorFactory visitorFactory : Arrays.<VisitorFactory>asList(
          (projectInfo, compilationUnit, file) -> new IndexSymbols(projectInfo, compilationUnit, file, index),
          VerifyMethodCalls::new,
          (projectInfo, compilationUnit, file) -> new VerifyAssignments(projectInfo, compilationUnit, file, index),
          VerifyFields::new,
          VerifyClosures::new,
          VerifyMethodVariables::new
      )) {
        try {
          rewrite(visitorFactory, isParallel(), true);
        } catch (CollectableException e) {
          exceptions.add(e);
        }
      }
      if (!exceptions.isEmpty()) {
        throw CollectableException.combine(exceptions);
      }
    }
  }
}
