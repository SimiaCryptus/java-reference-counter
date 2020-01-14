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

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

@RefIgnore
public class TestRefAutoCoder {
  @Nonnull
  public static ProjectInfo getProjectInfo() throws IOException, PlexusContainerException, ComponentLookupException, ProjectBuildingException, DependencyResolutionException {
    return SimpleMavenProject.load(new File("../demo").getCanonicalPath()).getProjectInfo();
  }

  @RefIgnore
  public static class Add {
    public static void main(String[] args) {
      try {
        new Insert.Coder(TestRefAutoCoder.getProjectInfo(), true).rewrite();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @RefIgnore
  public static class Remove {
    public static void main(String[] args) {
      try {
        new com.simiacryptus.ref.Remove.Coder(TestRefAutoCoder.getProjectInfo(), true).rewrite();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
