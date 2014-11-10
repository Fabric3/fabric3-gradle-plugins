/*
 * Fabric3
 * Copyright (c) 2009-2015 Metaform Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fabric3.gradle.plugin.core.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.internal.service.ServiceRegistry;

/**
 * Bootstraps the Aether infrastructure for resolving artifacts stored in Maven repositories.
 */
public class AetherBootstrap {

    public static RepositorySystem getRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace();
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    public static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system, ServiceRegistry registry, boolean offline) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        File file = new File(registry.get(RepositoryHandler.class).mavenLocal().getUrl().getPath());
        LocalRepository localRepo = new LocalRepository(file);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setCache(new DefaultRepositoryCache());
        session.setOffline(offline);
        // avoid unnecessary snapshot resolution
        session.setConfigProperty("aether.artifactResolver.snapshotNormalization", false);
        return session;
    }

    public static List<RemoteRepository> getRepositories(ServiceRegistry registry) {
        Iterator<ArtifactRepository> iterator = registry.get(RepositoryHandler.class).iterator();
        List<RemoteRepository> repositories = new ArrayList<>();
        while (iterator.hasNext()) {
            ArtifactRepository repository = iterator.next();
            String name = repository.getName();
            if (repository instanceof MavenArtifactRepository && !name.startsWith("MavenLocal")) {
                MavenArtifactRepository mavenRepository = (MavenArtifactRepository) repository;
                String url = mavenRepository.getUrl().toString();
                RemoteRepository.Builder builder = new RemoteRepository.Builder(name, "default", url);
                String proxyHost = System.getProperty("http.proxyHost");
                if (proxyHost != null) {
                    int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
                    builder.setProxy(new Proxy("http", proxyHost, proxyPort));
                }
                RemoteRepository remoteRepository = builder.build();
                repositories.add(remoteRepository);
            }
        }
        if (repositories.isEmpty()) {
            RemoteRepository remoteRepository = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
            repositories.add(remoteRepository);
        }
        return repositories;
    }

    private AetherBootstrap() {
    }
}
