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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.internal.java.JavaLibrary;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
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

}
