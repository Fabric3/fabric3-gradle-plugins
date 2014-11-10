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

/**
 *
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Helper methods for working with files.
 */
public class FileHelper {
    public static final int BUFFER = 2048;

    protected FileHelper() {
    }

    /**
     * Extracts the contents of a zip file to a target directory.
     *
     * @param source      the zip file
     * @param destination the target directory
     * @throws IOException if there is an error during extraction
     */
    public static void extract(File source, File destination) throws IOException {
        ZipFile zipfile;
        zipfile = new ZipFile(source);
        Enumeration enumeration = zipfile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) enumeration.nextElement();
            String name = entry.getName();
            if (entry.isDirectory()) {
                new File(destination, name).mkdirs();
            } else {
                if (name.toUpperCase().endsWith(".MF")) {
                    // ignore manifests
                    continue;
                }
                File outputFile = new File(destination, name);
                try (InputStream sourceStream = new BufferedInputStream(zipfile.getInputStream(entry));
                     OutputStream targetStream = new BufferedOutputStream(new FileOutputStream(outputFile), FileHelper.BUFFER)) {
                    FileHelper.copy(sourceStream, targetStream);
                    targetStream.flush();
                }
            }
        }
    }

    public static void copy(File source, File target) throws IOException {
        try (InputStream sourceStream = new BufferedInputStream(new FileInputStream(source));
             OutputStream targetStream = new BufferedOutputStream(new FileOutputStream(target))) {
            copy(sourceStream, targetStream);
        }
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER];
        int count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Delete a file. If file is a directory, delete it and all sub-directories.
     *
     * The difference between File.delete() and this method are: <ul> <li>A directory to be deleted does not have to be empty.</li> <li>You get exceptions when
     * a file or directory cannot be deleted. (java.io.File methods returns a boolean)</li> </ul>
     *
     * @param file file or directory to delete, not null
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist: " + file);
            }
            if (!file.delete()) {
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Recursively delete a directory.
     *
     * @param directory directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Clean a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (directory == null) {
            return;
        }
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) { // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

}