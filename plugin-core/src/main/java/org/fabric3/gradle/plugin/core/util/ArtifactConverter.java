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
package org.fabric3.gradle.plugin.core.util;

import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Converts an untyped artifact representation to a typed one.
 */
public class ArtifactConverter {

    public static Artifact convert(Map<String, String> extension, String type) {
        String group = extension.get("group");
        if (group == null) {
            throw new IllegalArgumentException("A group must be specified on an Fabric3 artifact definition");
        }
        String name = extension.get("name");
        if (name == null) {
            throw new IllegalArgumentException("A name must be specified on an Fabric3 artifact definition");
        }
        String version = extension.get("version");
        if (version == null) {
            throw new IllegalArgumentException("A version must be specified on an Fabric3 artifact definition");
        }
        if (type.equals("pom")) {
            return new DefaultArtifact(group, name, type, type, version);
        } else if (type.equals("zip")) {
            return new DefaultArtifact(group, name, "bin", "zip", version);
        }
        String archiveExtension = extension.get("extension");
        if (archiveExtension != null) {
            return new DefaultArtifact(group, name, archiveExtension, version);
        } else {
            return new DefaultArtifact(group, name, type, version);
        }
    }

    private ArtifactConverter() {
    }
}
