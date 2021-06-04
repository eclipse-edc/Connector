/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.testfixtures;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {
    public final static String SAMPLE_FILE_RESOURCE_NAME = "hello.txt";

    public static Path getResourcePath(String resourceName) {
        Path path = null;

        try {
            path = Paths.get(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            fail("Resource: " + resourceName + " does not exist" + e.getLocalizedMessage());
        }

        return path;
    }

    public static File getFileFromResourceName(String resourceName) {
        URI uri = null;
        try {
            uri = Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI();
        } catch (URISyntaxException e) {
            fail("Cannot proceed without File : " + resourceName);
        }

        return new File(uri);
    }
}
