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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.fabric3.gradle.plugin.core.Constants;
import org.fabric3.gradle.plugin.core.util.ConfigFile;
import org.gradle.api.Project;
import static org.fabric3.gradle.plugin.core.util.ArtifactConverter.convert;

/**
 *
 */
public class AssemblyPluginConvention {
    public static final String FABRIC3_ASSEMBLY_CONVENTION = "fabric3Assembly";

    private String systemConfig;

    private String runtimeVersion = Constants.FABRIC3_VERSION;

    /**
     * True if non-target runtime configurations should be removed.
     */
    private boolean clean;

    /**
     * Runtime configuration where the contributions should be copied.
     */
    private String contributionTarget = "vm";

    private Set<Artifact> extensions = new HashSet<>();
    private Set<Artifact> profiles = new HashSet<>();
    private Set<Artifact> exclusions = new HashSet<>();
    private Set<Artifact> datasources = new HashSet<>();
    private Set<Artifact> shared = new HashSet<>();
    private Set<Artifact> contributions = new HashSet<>();
    private Set<Project> projectContributions = new HashSet<>();
    private Set<ConfigFile> configFiles = new HashSet<>();

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public String getContributionTarget() {
        return contributionTarget;
    }

    public void setContributionTarget(String contributionTarget) {
        this.contributionTarget = contributionTarget;
    }

    public void shared(Map<String, String> extension) {
        shared.add(convert(extension, "jar"));
    }

    public void shared(String extension) {
        shared.add(new DefaultArtifact(extension));
    }

    public void extension(Map<String, String> extension) {
        extensions.add(convert(extension, "jar"));
    }

    public void extension(String extension) {
        extensions.add(new DefaultArtifact(extension));
    }

    public void exclude(Map<String, String> exclusion) {
        exclusions.add(convert(exclusion, "jar"));
    }

    public void exclude(String exclusion) {
        exclusions.add(new DefaultArtifact(exclusion));
    }

    public void profile(Map<String, String> profile) {
        profiles.add(convert(profile, "zip"));
    }

    public void profile(String profile) {
        profiles.add(new DefaultArtifact(profile));
    }

    public void contribution(Map<String, String> contribution) {
        contributions.add(convert(contribution, "jar"));
    }

    public void contribution(String contribution) {
        contributions.add(new DefaultArtifact(contribution));
    }

    public void contribution(Project project) {
        projectContributions.add(project);
    }

    public void datasources(Map<String, String> datasource) {
        datasources.add(convert(datasource, "jar"));
    }

    public void datasources(String datasource) {
        datasources.add(new DefaultArtifact(datasource));
    }

    public void configFile(Map<String, String> file) {
        String source = file.get("source");
        if (source == null) {
            throw new IllegalArgumentException("Source not specified for config file");
        }
        String target = file.get("target");
        configFiles.add(new ConfigFile(source, target));
    }

    public Set<Artifact> getContributions() {
        return contributions;
    }

    public Set<Project> getProjectContributions() {
        return projectContributions;
    }

    public Set<Artifact> getShared() {
        return shared;
    }

    public Set<Artifact> getExtensions() {
        return extensions;
    }

    public Set<Artifact> getProfiles() {
        return profiles;
    }

    public Set<Artifact> getExclusions() {
        return exclusions;
    }

    public Set<Artifact> getDatasources() {
        return datasources;
    }

    public Set<ConfigFile> getConfigFiles() {
        return configFiles;
    }

    public String getSystemConfig() {
        return systemConfig;
    }

    public void setSystemConfig(String systemConfig) {
        this.systemConfig = systemConfig;
    }

}
