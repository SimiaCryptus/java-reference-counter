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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class MavenUtil {
  public static MavenProject getMavenProject(File pom, @Nonnull final DefaultPlexusContainer container, final DefaultRepositorySystemSession session) throws ProjectBuildingException, ComponentLookupException {
    DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
    request.setRepositorySession(session);
    return container.lookup(ProjectBuilder.class).build(pom, request).getProject();
  }

  @Nonnull
  public static DefaultPlexusContainer getPlexusContainer(@Nonnull final File repositoryLocation) throws IOException, PlexusContainerException {
    DefaultRepositoryLayout defaultRepositoryLayout = new DefaultRepositoryLayout();
    ArtifactRepositoryPolicy repositoryPolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
    String url = "file://" + repositoryLocation.getCanonicalPath();
    ArtifactRepository repository = new MavenArtifactRepository("central", url, defaultRepositoryLayout, repositoryPolicy, repositoryPolicy);
    ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
    ContainerConfiguration configuration = new DefaultContainerConfiguration()
        .setClassWorld(classWorld).setRealm(classWorld.getClassRealm(null))
        .setClassPathScanning("index").setAutoWiring(true).setJSR250Lifecycle(true).setName("maven");
    return new DefaultPlexusContainer(configuration, new BasicModule(repository));
  }

  @Nonnull
  public static DefaultRepositorySystemSession getSession(final File repositoryLocation, final boolean isOffline, final Map<Object, Object> configProps, @Nonnull final DefaultPlexusContainer container) throws ComponentLookupException {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setConfigProperties(configProps);
    session.setCache(new DefaultRepositoryCache());
    session.setOffline(isOffline);
    session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
    session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(ResolutionErrorPolicy.CACHE_NOT_FOUND, ResolutionErrorPolicy.CACHE_NOT_FOUND));
    session.setArtifactTypeRegistry(RepositoryUtils.newArtifactTypeRegistry(container.lookup(ArtifactHandlerManager.class)));
    session.setLocalRepositoryManager(container.lookup(DefaultRepositorySystem.class).newLocalRepositoryManager(session, new LocalRepository(repositoryLocation)));
    return session;
  }
}
