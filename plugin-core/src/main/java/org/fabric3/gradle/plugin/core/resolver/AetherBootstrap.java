/*
 * Fabric3
 * Copyright (c) 2009-2013 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
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
                RemoteRepository remoteRepository = new RemoteRepository.Builder(name, "default", url).build();
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
