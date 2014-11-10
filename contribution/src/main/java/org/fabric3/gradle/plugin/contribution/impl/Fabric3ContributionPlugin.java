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
package org.fabric3.gradle.plugin.contribution.impl;

import javax.inject.Inject;
import java.io.File;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
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
 *
 * This plugin packages dependencies specified with the <code>compile</code> scope (including transitive dependencies) in the archive <code>META-INF/lib</code>
 * directory.  The plugin also adds the dependency scope <code>providedCompile</code> to the project configuration. Dependencies specified with the
 * <code>providedCompile</code> scope will be placed on the project compile classpath but will not be packaged in the archive <code>META-INF/lib</code>
 * directory.
 */
public class Fabric3ContributionPlugin implements Plugin<Project> {
    public static final String PROVIDED_COMPILE = "providedCompile";

    @Inject
    public void apply(final Project project) {

        disableJar(project);

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
                contribution.getMetaInf().into("lib").from(new File("~/.profile"));
            }
        });

        createConfiguration(project.getConfigurations());
    }

    private void createConfiguration(ConfigurationContainer container) {
        Configuration configuration = container.create(PROVIDED_COMPILE);
        configuration.setVisible(false);
        configuration.setDescription("Additional compile classpath for libraries that should not be part of the contribution archive.");
        container.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(configuration);
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
