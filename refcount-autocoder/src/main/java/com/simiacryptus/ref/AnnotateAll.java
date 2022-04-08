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

/**
 * This class is responsible for annotating all classes.
 *
 * @docgenVersion 9
 */
@RefIgnore
@Mojo(name = "annotate")
public class AnnotateAll extends RefAutoCoderMojo {
  /**
   * Returns an AutoCoder for the given ProjectInfo.
   *
   * @param projectInfo the ProjectInfo to get an AutoCoder for
   * @return an AutoCoder for the given ProjectInfo
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  protected AutoCoder getAutoCoder(ProjectInfo projectInfo) {
    return new Coder(projectInfo);
  }

  /**
   * The Coder class is a class that contains code.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class Coder extends AutoCoder {

    public Coder(ProjectInfo projectInfo) {
      super(projectInfo);
    }

    /**
     * Rewrites the code.
     *
     * @param isParallel true if the code should be run in parallel
     * @param rewriteAll true if all code should be rewritten
     * @docgenVersion 9
     */
    @Override
    public void rewrite() {
      rewrite(AnnotateAllMethodParams::new, isParallel(), true);
    }
  }
}
