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

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @param basedir          the directory where the pom.xml file is located
 * @param session          the Maven session object
 * @param project          the Maven project object
 * @param repositorySystem the RepositorySystem object
 * @author Some Author
 * @docgenVersion 9
 */
public abstract class BaseMojo extends AbstractMojo {
  @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
  protected File basedir;
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  protected MavenSession session;
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;
  @Component
  protected RepositorySystem repositorySystem;

  /**
   * Returns an array of dependencies for this object.
   *
   * @return an array of dependencies for this object
   * @docgenVersion 9
   */
  @Nonnull
  public String[] getDependencies() {
    if (project == null) return new String[]{};
    List<Dependency> dependencies = project.getDependencies();
    if (dependencies == null) return new String[]{};
    return dependencies.stream()
        .map(x -> toArtifact(x))
        .flatMap(x -> resolve(x).getArtifacts().stream())
        .map(artifact -> artifact.getFile())
        .filter(file -> null != file)
        .map(file -> file.getAbsolutePath())
        .distinct()
        .toArray(i -> new String[i]);
  }

  /**
   * @return an array of strings containing the sources
   * @throws NullPointerException if the array is null
   * @docgenVersion 9
   */
  @Nonnull
  public String[] getSources() {
    return Stream.concat(
        project.getTestCompileSourceRoots().stream(),
        project.getCompileSourceRoots().stream()
    ).filter(s -> new File(s).exists()).toArray(i -> new String[i]);
  }

  /**
   * Resolves the specified artifact.
   *
   * @param artifact The artifact to resolve.
   * @return The artifact resolution result.
   * @docgenVersion 9
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
   * @return an Optional containing the first Dependency object in the project's dependencies list whose groupId and artifactId match the parameters, or an empty Optional if no such Dependency is found
   * @Nonnull
   * @docgenVersion 9
   */
  @Nonnull
  protected Optional<Dependency> findDependency(String groupId, String artifactId) {
    return project.getDependencies().stream().filter(artifact ->
        artifact.getGroupId().equals(groupId) &&
            artifact.getArtifactId().equals(artifactId)).findAny();
  }

  /**
   * Converts a given dependency into an artifact.
   *
   * @param dependency the dependency to convert
   * @return the converted artifact
   * @docgenVersion 9
   */
  @Nonnull
  private Artifact toArtifact(@Nonnull Dependency dependency) {
    final ProjectArtifact artifact = new ProjectArtifact(project);
    artifact.setGroupId(dependency.getGroupId());
    artifact.setArtifactId(dependency.getArtifactId());
    return artifact;
  }
}
