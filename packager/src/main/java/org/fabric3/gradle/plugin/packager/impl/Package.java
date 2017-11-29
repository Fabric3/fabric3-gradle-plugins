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
package org.fabric3.gradle.plugin.packager.impl;

import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.fabric3.gradle.plugin.core.resolver.AetherBootstrap;
import org.fabric3.gradle.plugin.core.util.FileHelper;
import org.fabric3.gradle.plugin.core.util.ProgressLoggerCompat;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.War;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.ProgressLoggerFactory;
import static org.fabric3.gradle.plugin.core.Constants.FABRIC3_GROUP;
import static org.fabric3.gradle.plugin.core.Constants.FABRIC3_VERSION;

/**
 * Extends the War task to add packager-specific build tasks including runtime resolution, configuration, profile installation and extension installation.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Package extends War {
    public static final String F3_EXTENSIONS_JAR = "f3.extensions.jar";

    private ProgressLogger progressLogger;

    private File stagingDirectory;

    private RepositorySystem system;
    private DefaultRepositorySystemSession session;
    private List<RemoteRepository> repositories;
    private PackagerPluginConvention convention;
    private MetaClass metaClass;
    private File extensionsDirectory;

    @Inject
    public Package(ProgressLoggerFactory progressLoggerFactory) {
        this.progressLogger = progressLoggerFactory.newOperation("fabric3Packager");
    }

    protected void copy() {
        init();
        try {
            setExtension("war");
            File buildDirectory = getProject().getBuildDir();
            stagingDirectory = new File(buildDirectory, "f3");
            stagingDirectory.mkdirs();

            extensionsDirectory = new File(stagingDirectory, "extensions");
            extensionsDirectory.mkdir();

            installProfiles();
            installExtensions();

            File extensionsJar = createExtensionsArchive(extensionsDirectory, stagingDirectory);

            File nodeJar = resolve(new DefaultArtifact(FABRIC3_GROUP, "fabric3-node", "jar", FABRIC3_VERSION));
            File nodeExtensionsJar = resolve(new DefaultArtifact(FABRIC3_GROUP, "fabric3-node-extensions", "jar", FABRIC3_VERSION));

            getWebInf().into("lib").from(extensionsJar, nodeJar, nodeExtensionsJar);

            progressLogger.completed("COMPLETED");
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e);
        }
        super.copy();
    }

    private void init() {
        ProgressLoggerCompat.setDescription(progressLogger, "Fabric3 packager plugin");
        ProgressLoggerCompat.setLoggingHeader(progressLogger, "Fabric3 packager plugin");
        progressLogger.started("STARTING");

        Project project = getProject();
        boolean offline = project.getGradle().getStartParameter().isOffline();
        system = AetherBootstrap.getRepositorySystem();
        ServiceRegistry registry = getServices();
        session = AetherBootstrap.getRepositorySystemSession(system, registry, offline);
        repositories = AetherBootstrap.getRepositories(registry);

        File buildDir = project.getBuildDir();
        File imageDir = new File(buildDir, "image");
        imageDir.mkdirs();
        convention = (PackagerPluginConvention) project.getConvention().getByName(PackagerPluginConvention.FABRIC3_PACKAGER_CONVENTION);
    }

    private File createExtensionsArchive(File extensionsDirectory, File libDirectory) throws IOException {
        File archive = new File(libDirectory, F3_EXTENSIONS_JAR);
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(archive));
             JarOutputStream jarStream = new JarOutputStream(os)) {

            for (File file : extensionsDirectory.listFiles()) {
                if (!file.getName().endsWith(".jar")) {
                    // skip if not a jar or the library is included in the web app classpath (WEB-INF/lib)
                    continue;
                }
                JarEntry entry = new JarEntry(file.getName());
                jarStream.putNextEntry(entry);
                FileHelper.copy(new FileInputStream(file), jarStream);
            }
            jarStream.flush();
        }
        return archive;
    }

    private void installExtensions() throws IOException {
        for (Artifact artifact : convention.getExtensions()) {
            progressLogger.progress("Installing " + artifact.toString());
            File source = resolve(artifact);
            FileHelper.copy(source, new File(extensionsDirectory, source.getName()));
        }
    }

    private void installProfiles() throws IOException {
        for (Artifact profile : convention.getProfiles()) {
            progressLogger.progress("Installing " + profile.toString());
            FileHelper.extract(resolve(profile), stagingDirectory);
        }
    }

    private File resolve(Artifact artifact) {
        progressLogger.progress("Resolving " + artifact.toString());
        ArtifactRequest request = new ArtifactRequest();
        request.setRepositories(repositories);
        request.setArtifact(artifact);
        try {
            ArtifactResult resolvedRuntime = system.resolveArtifact(session, request);
            return resolvedRuntime.getArtifact().getFile();
        } catch (ArtifactResolutionException e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    public Object getProperty(String property) {
        return getMetaClass().getProperty(this, property);
    }

    public void setProperty(String property, Object newValue) {
        getMetaClass().setProperty(this, property, newValue);
    }

    public Object invokeMethod(String name, Object args) {
        return getMetaClass().invokeMethod(this, name, args);
    }

    public MetaClass getMetaClass() {
        if (metaClass == null) {
            metaClass = InvokerHelper.getMetaClass(getClass());
        }
        return metaClass;
    }

    public void setMetaClass(MetaClass metaClass) {
        this.metaClass = metaClass;
    }
}
