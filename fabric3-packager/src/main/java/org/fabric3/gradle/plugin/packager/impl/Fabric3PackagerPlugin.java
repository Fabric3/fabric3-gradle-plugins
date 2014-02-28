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
        convention.extension(Constants.FABRIC3_GROUP + ":" + "fabric3-node-servlet" + ":" + Constants.FABRIC3_VERSION);
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
