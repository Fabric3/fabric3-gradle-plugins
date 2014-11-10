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
package org.fabric3.gradle.plugin.assembly.impl;

import javax.inject.Inject;
import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.internal.java.JavaLibrary;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;

/**
 * Creates a Fabric3 runtime distribution.
 */
public class Fabric3AssemblyPlugin implements Plugin<Project> {
    /**
     * Marker that forces the Assembly task to be executed if no source files are present. If this marker is not added to the task input sources, the Gradle
     * <code>SkipEmptySourceFilesTaskExecuter</code> will skip execution of the task if the sources are empty.
     */
    private static final SimpleFileCollection REBUILD_MARKER = new SimpleFileCollection(new File("--"));

    @Inject
    public void apply(final Project project) {
        disableJar(project);
        project.getConvention().add(AssemblyPluginConvention.FABRIC3_ASSEMBLY_CONVENTION, AssemblyPluginConvention.class);

        Zip zip = project.getTasks().create("fabric3Assembly", Assemble.class);
        zip.setDescription("Assembles a Fabric3 runtime image.");
        zip.setGroup(BasePlugin.BUILD_GROUP);
        zip.getInputs().source(REBUILD_MARKER);

        JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
        zip.from(convention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());
        zip.setExtension("zip");
        Configuration runtimeConfiguration = project.getConfigurations().getByName("runtime");
        ArchivePublishArtifact artifact = new ArchivePublishArtifact(zip);
        runtimeConfiguration.getArtifacts().add(artifact);
        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);

        JavaLibrary library = new JavaLibrary(artifact, runtimeConfiguration.getAllDependencies());
        project.getComponents().add(library);

    }

    private void disableJar(Project project) {
        try {
            // disable the existing jar task to avoid overwriting the contribution plugin jar task output
            Jar jar = (Jar) project.getTasks().getByName("jar");
            jar.setEnabled(false);
        } catch (UnknownTaskException e) {
            // ignore
        }
    }

}
