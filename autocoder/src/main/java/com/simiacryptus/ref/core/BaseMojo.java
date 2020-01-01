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

package com.simiacryptus.ref.core;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The type Base mojo.
 */
public abstract class BaseMojo extends AbstractMojo {
  /**
   * The Basedir.
   */
  @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
  protected File basedir;
  /**
   * The Session.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  protected MavenSession session;
  /**
   * The Project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;
  /**
   * The Repository system.
   */
  @Component
  protected RepositorySystem repositorySystem;

  /**
   * Get dependencies string [ ].
   *
   * @return the string [ ]
   */
  @NotNull
  public String[] getDependencies() {
    return project.getDependencies().stream()
        .map(x -> toArtifact(x))
        .flatMap(x -> resolve(x).getArtifacts().stream())
        .map(org.apache.maven.artifact.Artifact::getFile)
        .map(File::getAbsolutePath)
        .distinct()
        .toArray(i -> new String[i]);
  }

  /**
   * Get sources string [ ].
   *
   * @return the string [ ]
   */
  @NotNull
  public String[] getSources() {
    return Stream.concat(
        project.getTestCompileSourceRoots().stream(),
        project.getCompileSourceRoots().stream()
    ).filter(s -> new File(s).exists()).toArray(i -> new String[i]);
  }

  /**
   * Resolve artifact resolution result.
   *
   * @param artifact the artifact
   * @return the artifact resolution result
   */
  public ArtifactResolutionResult resolve(Artifact artifact) {
    try {
      return repositorySystem.resolve(new ArtifactResolutionRequest()
          .setArtifact(artifact)
          .setResolveRoot(true)
          .setResolveTransitively(true)
          .setLocalRepository(session.getLocalRepository())
          .setRemoteRepositories(project.getRemoteArtifactRepositories()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Find dependency optional.
   *
   * @param groupId    the group id
   * @param artifactId the artifact id
   * @return the optional
   */
  @NotNull
  protected Optional<Dependency> findDependency(String groupId, String artifactId) {
    return project.getDependencies().stream().filter(artifact ->
        artifact.getGroupId().equals(groupId) &&
            artifact.getArtifactId().equals(artifactId)).findAny();
  }

  @NotNull
  private Artifact toArtifact(@NotNull Dependency dependency) {
    final ProjectArtifact artifact = new ProjectArtifact(project);
    artifact.setGroupId(dependency.getGroupId());
    artifact.setArtifactId(dependency.getArtifactId());
    return artifact;
  }
}