/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.junit.extensions.ClasspathReader;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;

import java.net.URL;

/**
 * Runtimes for E2E transfer test.
 * The usage of this pattern permits to initialize the classpath once for runtime in a lazy manner.
 * Tests will be quite faster this way because classpath loading (that requires interaction with gradlew) is a pretty slow activity.
 */
public enum Runtimes {

    IN_MEMORY_CONTROL_PLANE(
            ":system-tests:e2e-transfer-test:control-plane",
            ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client"
    ),

    IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE(
            ":system-tests:e2e-transfer-test:control-plane",
            ":system-tests:e2e-transfer-test:data-plane"
    ),

    IN_MEMORY_DATA_PLANE(
            ":system-tests:e2e-transfer-test:data-plane"
    ),

    POSTGRES_CONTROL_PLANE(
            ":system-tests:e2e-transfer-test:control-plane",
            ":dist:bom:controlplane-feature-sql-bom"
    ),

    POSTGRES_DATA_PLANE(
            ":system-tests:e2e-transfer-test:data-plane",
            ":dist:bom:dataplane-feature-sql-bom"
    );

    private URL[] classpathEntries;
    private final String[] modules;

    Runtimes(String... modules) {
        this.modules = modules;
    }

    public EmbeddedRuntime create(String name) {
        if (classpathEntries == null) {
            classpathEntries = ClasspathReader.classpathFor(modules);
        }
        return new EmbeddedRuntime(name, classpathEntries);
    }

}
