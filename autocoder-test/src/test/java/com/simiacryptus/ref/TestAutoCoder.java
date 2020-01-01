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

import com.simiacryptus.ref.core.PrintAST;
import com.simiacryptus.ref.core.ProjectInfo;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * The type Test auto coder.
 */
public class TestAutoCoder {
  @NotNull
  private ProjectInfo getProjectInfo() throws IOException, PlexusContainerException, ComponentLookupException, ProjectBuildingException, DependencyResolutionException {
    return SimpleMavenProject.load(new File("../demo").getCanonicalPath()).getProjectInfo();
  }

  /**
   * Print.
   *
   * @throws ComponentLookupException      the component lookup exception
   * @throws PlexusContainerException      the plexus container exception
   * @throws ProjectBuildingException      the project building exception
   * @throws DependencyResolutionException the dependency resolution exception
   * @throws IOException                   the io exception
   */
  @Test
  public void print() throws ComponentLookupException, PlexusContainerException, ProjectBuildingException, DependencyResolutionException, IOException {
    new PrintAST().getAutoCoder(TestAutoCoder.this.getProjectInfo()).rewrite();
  }

}