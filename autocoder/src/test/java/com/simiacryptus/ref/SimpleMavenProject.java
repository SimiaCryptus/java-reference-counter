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

package com.simiacryptus.ref;

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefCoderIgnore;
import org.apache.maven.project.*;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@RefCoderIgnore
public class SimpleMavenProject {
  private static final File repositoryLocation = new File(System.getProperty("user.home"), ".m2/repository");
  private static final Logger logger = LoggerFactory.getLogger(SimpleMavenProject.class);
  @NotNull
  public final DefaultPlexusContainer plexusContainer;
  @NotNull
  public final DefaultRepositorySystemSession session;
  public final MavenProject project;
  public final String projectRoot;

  public SimpleMavenProject(final String projectRoot) throws IOException, PlexusContainerException, ComponentLookupException, ProjectBuildingException {
    this.projectRoot = projectRoot;
    Map<Object, Object> configProps = new LinkedHashMap<>();
    configProps.put(ConfigurationProperties.USER_AGENT, "Maven+SimiaCryptus");
    configProps.put(ConfigurationProperties.INTERACTIVE, false);
    configProps.putAll(System.getProperties());
    this.plexusContainer = MavenUtil.getPlexusContainer(repositoryLocation);
    this.session = MavenUtil.getSession(repositoryLocation, false, configProps, plexusContainer);
    this.project = MavenUtil.getMavenProject(new File(projectRoot, "pom.xml"), plexusContainer, session);
  }

  @NotNull
  public static SimpleMavenProject load(String root) throws IOException, PlexusContainerException, ComponentLookupException, ProjectBuildingException, DependencyResolutionException {
    SimpleMavenProject mavenProject = new SimpleMavenProject(root);
    mavenProject.resolve().getDependencies().forEach((org.eclipse.aether.graph.Dependency dependency) -> {
      logger.info(String.format("Dependency: %s (%s)", dependency.getArtifact().getFile().getAbsolutePath(), dependency));
    });
    return mavenProject;
  }

  public String[] getDependencies() {
    return resolve().getDependencies().stream()
        .map(Dependency::getArtifact)
        .map(Artifact::getFile)
        .map(File::getAbsolutePath)
        .toArray(i -> new String[i]);
  }

  @NotNull
  public ProjectInfo getProjectInfo() {
    return new ProjectInfo(this.projectRoot, getSources(), getDependencies());
  }

  public String[] getSources() {
    return Stream.concat(
        project.getTestCompileSourceRoots().stream(),
        project.getCompileSourceRoots().stream()
    ).toArray(i -> new String[i]);
  }

  public DependencyResolutionResult resolve() {
    try {
      final ProjectDependenciesResolver resolver = plexusContainer.lookup(ProjectDependenciesResolver.class);
      return resolver.resolve(new DefaultDependencyResolutionRequest().setRepositorySession(session).setMavenProject(project));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
