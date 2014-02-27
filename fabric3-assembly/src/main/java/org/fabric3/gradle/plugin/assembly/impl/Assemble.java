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
package org.fabric3.gradle.plugin.assembly.impl;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.ProgressLoggerFactory;

/**
 * Extends the Zip task to add assembly-specific build tasks including runtime resolution, configuration, profile installation and extension installation.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Assemble extends Zip {
    public static final String FABRIC3_GROUP = "org.codehaus.fabric3";
    private static final int BUFFER = 2048;

    private ProgressLogger progressLogger;
    private File imageDir;
    private RepositorySystem system;
    private DefaultRepositorySystemSession session;
    private List<RemoteRepository> repositories;
    private AssemblyPluginConvention convention;

    @Inject
    public Assemble(ProgressLoggerFactory progressLoggerFactory) {
        this.progressLogger = progressLoggerFactory.newOperation("fabric3Assembly");
    }

    protected void copy() {
        init();
        try {
            installRuntime();
            installProfiles();
            installExtensions();
            installDatasources();
            installContributions();
            installConfiguration();
            removeExtensions();

            if (convention.isClean()) {
                cleanRuntimes(imageDir);
            }

            from(imageDir);
            progressLogger.completed("COMPLETED");
        } catch (ArtifactResolutionException | IOException e) {
            throw new GradleException(e.getMessage(), e);
        }
        super.copy();
    }

    private void init() {
        progressLogger.setDescription("Fabric3 assembly plugin");
        progressLogger.setLoggingHeader("Fabric3 assembly plugin");
        progressLogger.started("STARTING");

        Project project = getProject();
        boolean offline = project.getGradle().getStartParameter().isOffline();
        system = AetherBootstrap.getRepositorySystem();
        ServiceRegistry registry = getServices();
        session = AetherBootstrap.getRepositorySystemSession(system, registry, offline);
        repositories = AetherBootstrap.getRepositories(registry);

        File buildDir = project.getBuildDir();
        imageDir = new File(buildDir, "image");
        imageDir.mkdirs();
        convention = (AssemblyPluginConvention) project.getConvention().getByName(AssemblyPluginConvention.FABRIC3_ASSEMBLY_CONVENTION);
    }

    /**
     * Cleans the runtime directories.
     *
     * @param imageDir root runtime image
     */
    private void cleanRuntimes(File imageDir) throws IOException {
        File runtimes = new File(imageDir, "runtimes");
        for (File file : runtimes.listFiles()) {
            if (file.isDirectory() && !convention.getContributionTarget().equals(file.getName())) {
                FileHelper.forceDelete(file);
            }
        }
    }

    private void removeExtensions() {
        for (Artifact extension : convention.getExclusions()) {
            progressLogger.progress("Excluding " + extension.toString());
            String id = extension.getArtifactId();
            String version = extension.getVersion();
            String fileName = id + "-" + version + ".jar";
            File extensionsDir = new File(imageDir, "extensions");
            File file = new File(extensionsDir, fileName);
            boolean result = file.delete();
            if (!result) {
                throw new GradleException("Unable to exclude extension: " + file);
            }
        }

    }

    private void installConfiguration() throws IOException {
        for (ConfigFile file : convention.getConfigFiles()) {
            // main directory is parent of the build directory
            File source = new File(getProject().getBuildDir().getParent(), file.getSource());
            File targetDir = new File(imageDir, file.getDestination());
            File target = new File(targetDir, source.getName());
            target.mkdirs();
            copy(source, target);
        }
    }

    private void installContributions() throws ArtifactResolutionException, IOException {
        File repository = new File(imageDir, "runtimes" + File.separator + convention.getContributionTarget() + File.separatorChar + "deploy");
        repository.mkdirs();

        for (Artifact artifact : convention.getContributions()) {
            progressLogger.progress("Installing " + artifact.toString());
            ArtifactResult result = system.resolveArtifact(session, new ArtifactRequest(artifact, null, null));

            File source = result.getArtifact().getFile();

            File target = new File(repository, source.getName());
            copy(source, target);
        }

        for (Project project : convention.getProjectContributions()) {
            progressLogger.progress("Installing " + project.getName());
            File[] files = new File(project.getBuildDir() + File.separator + "libs").listFiles();
            File source;
            if (files == null || files.length == 0) {
                throw new GradleException("Archive not found for contribution project: " + project.getName());
            } else if (files.length > 1) {
                // More than one archive. Check if a WAR is produced and use that as sometimes the JAR task may not be disabled in a webapp project, resulting
                // in multiple artifacts.
                int war = -1;
                for (File file : files) {
                    if (file.getName().endsWith(".war")) {
                        war++;
                        break;
                    }
                }
                if (war == -1) {
                    throw new GradleException("Contribution project has multiple library archives: " + project.getName());
                }
                source = files[war];
            } else {
                source = files[0];
            }
            File target = new File(repository, source.getName());
            copy(source, target);
        }
    }

    private void installDatasources() throws ArtifactResolutionException, IOException {
        if (convention.getDatasources().isEmpty()) {
            return;
        }

        File extensionsDir = new File(imageDir, "extensions");
        File datasourceDir = new File(extensionsDir, "datasource");

        datasourceDir.mkdirs();
        for (Artifact artifact : convention.getDatasources()) {
            progressLogger.progress("Installing " + artifact.toString());
            ArtifactResult result = system.resolveArtifact(session, new ArtifactRequest(artifact, null, null));
            File source = result.getArtifact().getFile();
            File target = new File(datasourceDir, source.getName());
            copy(source, target);
        }

    }

    private void installExtensions() throws IOException {
        File extensionDir = new File(imageDir, "extensions");
        for (Artifact artifact : convention.getExtensions()) {
            progressLogger.progress("Installing " + artifact.toString());
            File source = resolve(artifact);
            copy(source, new File(extensionDir, source.getName()));
        }
    }

    private void installProfiles() throws IOException {
        for (Artifact profile : convention.getProfiles()) {
            progressLogger.progress("Installing " + profile.toString());
            extract(resolve(profile), imageDir);
        }
    }

    private void installRuntime() throws IOException {
        progressLogger.progress("Installing the runtime");
        DefaultArtifact runtimeArtifact = new DefaultArtifact(FABRIC3_GROUP, "runtime-standalone", "bin", "zip", convention.getRuntimeVersion());
        extract(resolve(runtimeArtifact), imageDir);
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

    /**
     * Extracts the contents of a zip file to a target directory.
     *
     * @param source      the zip file
     * @param destination the target directory
     * @throws IOException if there is an error during extraction
     */
    private void extract(File source, File destination) throws IOException {
        ZipFile zipfile;
        zipfile = new ZipFile(source);
        Enumeration enumeration = zipfile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) enumeration.nextElement();
            String name = entry.getName();
            if (entry.isDirectory()) {
                new File(destination, name).mkdirs();
            } else {
                if (name.toUpperCase().endsWith(".MF")) {
                    // ignore manifests
                    continue;
                }
                File outputFile = new File(destination, name);
                try (InputStream sourceStream = new BufferedInputStream(zipfile.getInputStream(entry));
                     OutputStream targetStream = new BufferedOutputStream(new FileOutputStream(outputFile), BUFFER)) {
                    copy(sourceStream, targetStream);
                    targetStream.flush();
                }
            }
        }
    }

    private void copy(File source, File target) throws IOException {
        try (InputStream sourceStream = new BufferedInputStream(new FileInputStream(source));
             OutputStream targetStream = new BufferedOutputStream(new FileOutputStream(target))) {
            copy(sourceStream, targetStream);
        }
    }

    private int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER];
        int count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}
