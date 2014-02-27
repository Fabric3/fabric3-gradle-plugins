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
package org.fabric3.gradle.plugin.contribution.impl;

import javax.inject.Inject;
import java.io.File;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.java.JavaLibrary;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

/**
 * Packages a Gradle project as a Fabric3 JAR contribution.
 * <p/>
 * This plugin packages dependencies specified with the <code>compile</code> scope (including transitive dependencies) in the archive <code>META-INF/lib</code>
 * directory.  The plugin also adds the dependency scope <code>providedCompile</code> to the project configuration. Dependencies specified with the
 * <code>providedCompile</code> scope will be placed on the project compile classpath but will not be packaged in the archive <code>META-INF/lib</code>
 * directory.
 */
public class Fabric3ContributionPlugin implements Plugin<Project> {
    public static final String PROVIDED_COMPILE = "providedCompile";

    @Inject
    public void apply(final Project project) {
        final Jar contribution = project.getTasks().create("fabric3Contribution", Jar.class);
        contribution.setDescription("Assembles a contribution archive containing the main classes and library dependencies.");
        contribution.setGroup(BasePlugin.BUILD_GROUP);

        JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
        contribution.from(convention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());

        Configuration runtimeConfiguration = project.getConfigurations().getByName("runtime");
        ArchivePublishArtifact jarArtifact = new ArchivePublishArtifact(contribution);
        runtimeConfiguration.getArtifacts().add(jarArtifact);
        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(jarArtifact);

        JavaLibrary library = new JavaLibrary(jarArtifact, runtimeConfiguration.getAllDependencies());
        project.getComponents().add(library);

        // delay copying libraries until after the project classpath has been resolved
        project.afterEvaluate(new Action<Project>() {
            public void execute(Project project) {
                FileTree compileTree = project.getConfigurations().getByName("compile").getAsFileTree();
                Configuration providedCompile = project.getConfigurations().getByName(PROVIDED_COMPILE);
                Set<File> libraries = compileTree.minus(providedCompile).getFiles();
                contribution.getMetaInf().into("lib").from(libraries);
            }
        });

        createConfiguration(project.getConfigurations());
    }

    public void createConfiguration(ConfigurationContainer container) {
        Configuration configuration = container.create(PROVIDED_COMPILE);
        configuration.setVisible(false);
        configuration.setDescription("Additional compile classpath for libraries that should not be part of the contribution archive.");
        container.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(configuration);
    }

}
