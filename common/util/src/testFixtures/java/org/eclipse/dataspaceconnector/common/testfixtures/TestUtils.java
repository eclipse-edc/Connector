/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.common.testfixtures;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {
    public static final String SAMPLE_FILE_RESOURCE_NAME = "hello.txt";
    public static final int MAX_TCP_PORT = 65_535;

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

    public static String getResourceFileContentAsString(String resourceName) {
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        Scanner s = new Scanner(Objects.requireNonNull(stream)).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Gets a free port in the range 1024 - 65535 by trying them in ascending order.
     *
     * @return the first free port
     * @throws IllegalArgumentException if no free port is available
     */
    public static int getFreePort() {
        var rnd = 1024 + new Random().nextInt(MAX_TCP_PORT - 1024);
        return getFreePort(rnd);
    }

    /**
     * Gets a free port in the range lowerBound - 65535 by trying them in ascending order.
     *
     * @return the first free port
     * @throws IllegalArgumentException if no free port is available
     */
    public static int getFreePort(int lowerBound) {
        if (lowerBound <= 0 || lowerBound >= MAX_TCP_PORT) {
            throw new IllegalArgumentException("Lower bound must be > 0 and < " + MAX_TCP_PORT);
        }
        return getFreePort(lowerBound, MAX_TCP_PORT);
    }

    /**
     * Gets a free port in the range lowerBound - upperBound by trying them in ascending order.
     *
     * @return the first free port
     * @throws IllegalArgumentException if no free port is available or if the bounds are invalid.
     */
    public static int getFreePort(int lowerBound, int upperBound) {

        if (lowerBound <= 0 || lowerBound >= MAX_TCP_PORT || lowerBound >= upperBound) {
            throw new IllegalArgumentException("Lower bound must be > 0 and < " + MAX_TCP_PORT + " and be < upperBound");
        }
        if (upperBound > MAX_TCP_PORT) {
            throw new IllegalArgumentException("Upper bound must be < " + MAX_TCP_PORT);
        }
        var port = lowerBound;
        boolean found = false;

        while (!found && port <= upperBound) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setReuseAddress(true);
                port = serverSocket.getLocalPort();

                found = true;
            } catch (IOException e) {
                found = false;
                port++;
            }
        }

        if (!found) {
            throw new IllegalArgumentException(format("No free ports in the range [%d - %d]", lowerBound, upperBound));
        }
        return port;
    }
}
