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
