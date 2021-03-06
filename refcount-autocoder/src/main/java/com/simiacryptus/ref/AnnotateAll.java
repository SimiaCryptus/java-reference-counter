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
import com.simiacryptus.ref.ops.AnnotateAllMethodParams;
import org.apache.maven.plugins.annotations.Mojo;

import javax.annotation.Nonnull;

@RefIgnore
@Mojo(name = "annotate")
public class AnnotateAll extends RefAutoCoderMojo {
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
    public void rewrite() {
      rewrite(AnnotateAllMethodParams::new, isParallel(), true);
    }
  }
}
