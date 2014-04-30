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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.fabric3.gradle.plugin.core.resolver.AetherBootstrap;
import org.fabric3.gradle.plugin.core.stopwatch.NoOpStopWatch;
import org.fabric3.gradle.plugin.core.stopwatch.StopWatch;
import org.fabric3.gradle.plugin.core.stopwatch.StreamStopWatch;
import org.fabric3.gradle.plugin.core.util.ConfigFile;
import org.fabric3.gradle.plugin.core.util.FileHelper;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.ProgressLoggerFactory;
import static org.fabric3.gradle.plugin.core.Constants.FABRIC3_GROUP;

/**
 * Extends the Zip task to add assembly-specific build tasks including runtime resolution, configuration, profile installation and extension installation.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Assemble extends Zip {

    private StopWatch stopWatch;
    private ProgressLogger progressLogger;
    private File imageDir;
    private RepositorySystem system;
    private DefaultRepositorySystemSession session;
    private List<RemoteRepository> repositories;
    private AssemblyPluginConvention convention;

    @Inject
    public Assemble(ProgressLoggerFactory progressLoggerFactory) {
        this.progressLogger = progressLoggerFactory.newOperation("fabric3Assembly");
        if (Boolean.parseBoolean(System.getProperty("fabric3.performance"))) {
            stopWatch = new StreamStopWatch("gradle", TimeUnit.MILLISECONDS, System.out);
        } else {
            stopWatch = new NoOpStopWatch();
        }
    }

    protected void copy() {
        stopWatch.start();
        init();
        try {
            installRuntime();
            installShared();
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
        stopWatch.stop();
        stopWatch.flush();
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
        stopWatch.split("Fabric3 Assembly init");
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
        File defaultTargetDir = new File(imageDir, "runtimes" + File.separatorChar + convention.getContributionTarget() + File.separatorChar + "config");
        for (ConfigFile file : convention.getConfigFiles()) {
            // main directory is parent of the build directory
            File source = new File(getProject().getBuildDir().getParent(), file.getSource());
            // if no target specified, use the default runtime config directory
            File targetDir;
            String destination = file.getDestination();
            if (destination == null) {
                targetDir = defaultTargetDir;
            } else {
                targetDir = new File(imageDir, destination);
            }
            File target = new File(targetDir, source.getName());
            targetDir.mkdirs();
            FileHelper.copy(source, target);
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
            FileHelper.copy(source, target);
        }

        stopWatch.split("Fabric3 Assembly resolve and install contributions");

        for (Project project : convention.getProjectContributions()) {
            progressLogger.progress("Installing " + project.getName());
            File[] files = new File(project.getBuildDir() + File.separator + "libs").listFiles();
            File source = null;
            if (files == null || files.length == 0) {
                throw new GradleException("Archive not found for contribution project: " + project.getName());
            } else if (files.length > 1) {
                // More than one archive. Check if a WAR is produced and use that as sometimes the JAR task may not be disabled in a webapp project, resulting
                // in multiple artifacts.
                for (File file : files) {
                    if (file.getName().endsWith(".war")) {
                        source = file;
                        break;
                    }
                }
                if (source == null) {
                    throw new GradleException("Contribution project has multiple library archives: " + project.getName());
                }
            } else {
                source = files[0];
            }
            File target = new File(repository, source.getName());
            FileHelper.copy(source, target);
        }
        stopWatch.split("Fabric3 Assembly install project contributions");
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
            FileHelper.copy(source, target);
        }
        stopWatch.split("Fabric3 Assembly resolve and extract datasource extensions");
    }

    private void installExtensions() throws IOException {
        File extensionDir = new File(imageDir, "extensions");
        for (Artifact artifact : convention.getExtensions()) {
            progressLogger.progress("Installing " + artifact.toString());
            File source = resolve(artifact);
            FileHelper.copy(source, new File(extensionDir, source.getName()));
        }
        stopWatch.split("Fabric3 Assembly resolve and copy extensions");
    }

    private void installShared() throws IOException {
        File hostDir = new File(imageDir, "host");
        for (Artifact artifact : convention.getShared()) {
            progressLogger.progress("Installing " + artifact.toString());
            File source = resolve(artifact);
            FileHelper.copy(source, new File(hostDir, source.getName()));
        }
        stopWatch.split("Fabric3 Assembly resolve and copy shared artifacts");
    }

    private void installProfiles() throws IOException {
        for (Artifact profile : convention.getProfiles()) {
            progressLogger.progress("Installing " + profile.toString());
            FileHelper.extract(resolve(profile), imageDir);
        }
        stopWatch.split("Fabric3 Assembly resolve and extract profiles");
    }

    private void installRuntime() throws IOException {
        progressLogger.progress("Installing the runtime");
        DefaultArtifact runtimeArtifact = new DefaultArtifact(FABRIC3_GROUP, "runtime-standalone", "bin", "zip", convention.getRuntimeVersion());
        File resolved = resolve(runtimeArtifact);
        stopWatch.split("Fabric3 Assembly resolve runtime distribution");
        FileHelper.extract(resolved, imageDir);
        stopWatch.split("Fabric3 Assembly extract runtime distribution");
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

}
