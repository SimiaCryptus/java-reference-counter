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
import com.simiacryptus.ref.ops.InlineRefs;
import com.simiacryptus.ref.ops.InlineTempVars;
import com.simiacryptus.ref.ops.RemoveRefs;
import org.apache.maven.plugins.annotations.Mojo;

import javax.annotation.Nonnull;

/**
 * The Remove class is used to remove elements from an ArrayList.
 *
 * @docgenVersion 9
 */
@RefIgnore
@Mojo(name = "remove")
public class Remove extends RefAutoCoderMojo {
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
    return new Coder(projectInfo, getBoolean("modifyAPI", false));
  }

  /**
   * A class that represents a coder.
   *
   * @param shouldChangeAPI A boolean value that determines whether or not the API should be changed.
   * @docgenVersion 9
   */
  @RefIgnore
  public static class Coder extends AutoCoder {
    private final boolean shouldChangeAPI;

    public Coder(ProjectInfo projectInfo, boolean shouldChangeAPI) {
      super(projectInfo);
      this.shouldChangeAPI = shouldChangeAPI;
    }

    /**
     * @Override public void rewrite();
     * @docgenVersion 9
     */
    @Override
    public void rewrite() {
      while (rewrite(RemoveRefs.ModifyBlock::new) + rewrite(RemoveRefs.ModifyMethodInvocation::new) > 0) {
        logger.info("Re-running RemoveRefs");
      }
      while (rewrite(InlineRefs.ModifyAssignment::new) + rewrite(InlineRefs.ModifyBlock::new) + rewrite(InlineRefs.ModifyReturnStatement::new) > 0) {
        logger.info("Re-running InlineRefs");
      }
      while (rewrite(InlineTempVars::new) > 0) {
        logger.info("Re-running InlineTempVars");
      }
      if (shouldChangeAPI) {
        new RevertAPI.Coder(projectInfo).rewrite();
      }
    }
  }
}
