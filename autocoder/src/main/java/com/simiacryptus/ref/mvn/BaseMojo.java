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

package com.simiacryptus.ref.mvn;

import com.simiacryptus.ref.core.ProjectInfo;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.stream.Stream;

public abstract class BaseMojo extends AbstractMojo {
  @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
  private File basedir;
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;
  @Component
  private RepositorySystem repositorySystem;

  public String[] getDependencies() {
    return resolve().getArtifacts().stream()
        .map(org.apache.maven.artifact.Artifact::getFile)
        .map(File::getAbsolutePath)
        .toArray(i -> new String[i]);
  }

  @NotNull
  public ProjectInfo getProjectInfo() {
    return new ProjectInfo(basedir.getAbsolutePath(), getSources(), getDependencies());
  }

  public String[] getSources() {
    return Stream.concat(
        project.getTestCompileSourceRoots().stream(),
        project.getCompileSourceRoots().stream()
    ).toArray(i -> new String[i]);
  }

  public ArtifactResolutionResult resolve() {
    try {
      return repositorySystem.resolve(new ArtifactResolutionRequest()
          .setArtifact(project.getArtifact())
          .setResolveRoot(true)
          .setResolveTransitively(true)
          .setLocalRepository(session.getLocalRepository())
          .setRemoteRepositories(project.getRemoteArtifactRepositories()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
