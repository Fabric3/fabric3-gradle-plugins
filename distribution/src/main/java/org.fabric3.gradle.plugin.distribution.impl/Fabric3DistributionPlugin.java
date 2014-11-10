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
package org.fabric3.gradle.plugin.distribution.impl;

import javax.inject.Inject;

import groovy.lang.Closure;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.internal.DefaultDistributionContainer;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Tar;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.internal.reflect.Instantiator;

/**
 * Plugin that builds distributions. This plugin is similar to the standard Gradle plugin except it allows contents to be placed in the root archive folder and
 * does not include an install task.
 */
public class Fabric3DistributionPlugin implements Plugin<Project> {

    private static final String MAIN_DISTRIBUTION_NAME = "main";
    private static final String DISTRIBUTION_GROUP = "distribution";
    private static final String TASK_DIST_ZIP_NAME = "distZip";
    private static final String TASK_DIST_TAR_NAME = "distTar";

    private final Instantiator instantiator;
    private final FileOperations fileOperations;

    @Inject
    Fabric3DistributionPlugin(Instantiator instantiator, FileOperations fileOperations) {
        this.fileOperations = fileOperations;
        this.instantiator = instantiator;
    }

    public void apply(final Project project) {
        project.getPlugins().apply(BasePlugin.class);

        ExtensionContainer extensions = project.getExtensions();
        DefaultDistributionContainer distributions = extensions.create("distributions",
                                                                       DefaultDistributionContainer.class,
                                                                       Distribution.class,
                                                                       instantiator,
                                                                       fileOperations);

        distributions.all(new Closure<Object>(distributions) {

            public Object call(final Object... args) {
                Distribution distribution = (Distribution) args[0];
                String name = distribution.getName();
                distribution.setBaseName(name.equals(MAIN_DISTRIBUTION_NAME) ? project.getName() : String.format("%s-%s", project.getName(), name));
                distribution.getContents().from("src/$dist.name/dist");

                addZipTask(project, distribution);
                addTarTask(project, distribution);
                return args;
            }

            public int getMaximumNumberOfParameters() {
                return 1;
            }
        });
        distributions.create(MAIN_DISTRIBUTION_NAME);
    }

    void addZipTask(Project project, Distribution distribution) {
        String taskName = TASK_DIST_ZIP_NAME;
        if (!MAIN_DISTRIBUTION_NAME.equals(distribution.getName())) {
            taskName = distribution.getName() + "DistZip";
        }
        configureArchiveTask(project, taskName, distribution, Zip.class);
    }

    void addTarTask(Project project, Distribution distribution) {
        String taskName = TASK_DIST_TAR_NAME;
        if (!MAIN_DISTRIBUTION_NAME.equals(distribution.getName())) {
            taskName = distribution.getName() + "DistTar";
        }
        configureArchiveTask(project, taskName, distribution, Tar.class);
    }

    private <T extends AbstractArchiveTask> void configureArchiveTask(Project project, String taskName, final Distribution distribution, Class<T> type) {
        AbstractArchiveTask archiveTask = project.getTasks().create(taskName, type);
        archiveTask.setDescription("Bundles the project as a distribution.");
        archiveTask.setGroup(DISTRIBUTION_GROUP);
        archiveTask.getConventionMapping().map("baseName", new Closure<String>(this) {
            public String call() {
                if (distribution.getBaseName() == null || distribution.getBaseName().equals("")) {
                    throw new GradleException("Distribution baseName must not be null or empty! Check your configuration of the distribution plugin.");
                }
                return distribution.getBaseName();
            }

            public int getMaximumNumberOfParameters() {
                return 0;
            }
        });
        archiveTask.into("").with((distribution.getContents()));
    }

}
