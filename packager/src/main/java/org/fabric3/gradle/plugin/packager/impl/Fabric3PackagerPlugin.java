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
import java.util.concurrent.Callable;

import org.fabric3.gradle.plugin.core.Constants;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.java.JavaLibrary;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.War;
import static org.fabric3.gradle.plugin.packager.impl.PackagerPluginConvention.FABRIC3_PACKAGER_CONVENTION;

/**
 * Packages a Fabric3 node runtime distribution.
 */
public class Fabric3PackagerPlugin implements Plugin<Project> {
    public static final String PROVIDED_COMPILE = "providedCompile";
    public static final String PROVIDED_RUNTIME_CONFIGURATION_NAME = "providedRuntime";

    @Inject
    public void apply(final Project project) {
        disableJar(project);

        final PackagerPluginConvention convention = new PackagerPluginConvention(project);
        project.getConvention().add(FABRIC3_PACKAGER_CONVENTION, convention);

        addDefaultExtensions(convention);

        War zip = project.getTasks().create("fabric3Packager", Package.class);
        zip.setDescription("Packages a Fabric3 node runtime image.");
        zip.setGroup(BasePlugin.BUILD_GROUP);

        Configuration runtimeConfiguration = project.getConfigurations().getByName("runtime");
        ArchivePublishArtifact artifact = new ArchivePublishArtifact(zip);
        runtimeConfiguration.getArtifacts().add(artifact);
        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);

        JavaLibrary library = new JavaLibrary(artifact, runtimeConfiguration.getAllDependencies());
        project.getComponents().add(library);

        project.getTasks().withType(War.class, new Action<War>() {
            public void execute(War task) {
                task.from(new Callable() {
                    public Object call() throws Exception {
                        return convention.getWebAppDir();
                    }
                });
                task.dependsOn(new Callable() {
                    public Object call() throws Exception {
                        return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                                .getRuntimeClasspath();
                    }
                });
                task.classpath(new Callable() {
                    public Object call() throws Exception {
                        FileCollection runtimeClasspath
                                = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                                .getRuntimeClasspath();
                        Configuration providedRuntime = project.getConfigurations().getByName(PROVIDED_RUNTIME_CONFIGURATION_NAME);
                        return runtimeClasspath.minus(providedRuntime);
                    }
                });
            }
        });

        createConfiguration(project.getConfigurations());
    }

    public void createConfiguration(ConfigurationContainer container) {
        Configuration configuration = container.create(PROVIDED_COMPILE);
        configuration.setVisible(false);
        configuration.setDescription("Additional compile classpath for libraries that should not be part of the contribution archive.");
        container.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(configuration);

        Configuration provideRuntimeConfiguration = container.create(PROVIDED_RUNTIME_CONFIGURATION_NAME).setVisible(false).
                extendsFrom(configuration).
                setDescription("Additional runtime classpath for libraries that should not be part of the WAR archive.");
        container.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME).extendsFrom(provideRuntimeConfiguration);

    }

    private void addDefaultExtensions(PackagerPluginConvention convention) {
        convention.extension(Constants.FABRIC3_GROUP + ":" + "fabric3-databinding-json" + ":" + Constants.FABRIC3_VERSION);
    }

    private void disableJar(Project project) {
        try {
            // disable the existing jar task to avoid overwriting the contribution plugin war task output
            Jar jar = (Jar) project.getTasks().getByName("jar");
            jar.setEnabled(false);
        } catch (UnknownTaskException e) {
            // ignore
        }
    }

}
