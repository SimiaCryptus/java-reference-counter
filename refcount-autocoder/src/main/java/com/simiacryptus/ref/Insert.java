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

import javax.annotation.Nonnull;

@RefIgnore
@Mojo(name = "insert")
public class Insert extends RefAutoCoderMojo {
  @Nonnull
  @Override
  protected AutoCoder getAutoCoder(ProjectInfo projectInfo) {
    return new Coder(projectInfo, getBoolean("modifyAPI", false));
  }

  @RefIgnore
  public static class Coder extends AutoCoder {
    private final boolean shouldChangeAPI;

    public Coder(ProjectInfo projectInfo, boolean shouldChangeAPI) {
      super(projectInfo);
      this.shouldChangeAPI = shouldChangeAPI;
    }

    @Override
    public void rewrite() {
      new Check.Coder(projectInfo).rewrite();
      new Remove().getAutoCoder(projectInfo).rewrite();
      if (shouldChangeAPI) {
        new ModifyAPI.Coder(projectInfo).rewrite();
      } else {
        rewrite(RemoveRefMethods::new);
        rewrite(InsertMethods.ModifyAnonymousClassDeclaration::new);
        rewrite(InsertMethods.ModifyTypeDeclaration::new);
      }
      rewrite(InsertAddRefs.ModifyArrayInitializer::new);
      rewrite(InsertAddRefs.ModifyClassInstanceCreation::new);
      rewrite(InsertAddRefs.ModifyConstructorInvocation::new);
      rewrite(InsertAddRefs.ModifyMethodInvocation::new);
      rewrite(InsertAddRefs.ModifyReturnStatement::new);
      rewrite(InsertAddRefs.ModifyAssignment::new);
      rewrite(InsertAddRefs.ModifyVariableDeclarationFragment::new);
      rewrite(ModifyAssignments::new);
      rewrite(InsertFreeRefs.ModifyVariableDeclarationFragment::new);
      rewrite(InsertFreeRefs.ModifySingleVariableDeclaration::new);
      rewrite(InsertFreeRefs.ModifyClassInstanceCreation::new);
      rewrite(InsertFreeRefs.ModifyMethodInvocation::new);
      rewrite(InstrumentClosures.ModifyAnonymousClassDeclaration::new);
      rewrite(InstrumentClosures.ModifyLambdaExpression::new);
      rewrite(OptimizeRefs::new);
    }
  }
}
