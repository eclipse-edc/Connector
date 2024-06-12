/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.junit.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A JUnit extension for running an embedded EDC runtime as part of a test fixture. A custom gradle task printClasspath
 * is used to determine the runtime classpath of the module to run. The runtime obtains a classpath determined by the
 * Gradle build.
 * <p>
 * This extension attaches an EDC runtime to the {@link BeforeTestExecutionCallback} and
 * {@link AfterTestExecutionCallback} lifecycle hooks. Parameter injection of runtime services is supported.
 *
 * @deprecated please use either {@link RuntimePerMethodExtension} or {@link RuntimePerClassExtension}.
 */
@Deprecated(since = "0.7.0")
public class EdcRuntimeExtension extends EdcExtension {

    /**
     * Initialize an Edc runtime given a base runtime module
     *
     * @param baseModulePath    the base runtime module path
     * @param name              the name.
     * @param properties        the properties to be used as configuration.
     * @param additionalModules modules that will be added to the runtime.
     */
    public EdcRuntimeExtension(String baseModulePath, String name, Map<String, String> properties, String... additionalModules) {
        this(name, properties, Stream.concat(Stream.of(baseModulePath), Arrays.stream(additionalModules)).toArray(String[]::new));
    }

    /**
     * Initialize an Edc runtime
     *
     * @param name       the name.
     * @param properties the properties to be used as configuration.
     * @param modules    the modules that will be used to load the runtime.
     */
    public EdcRuntimeExtension(String name, Map<String, String> properties, String... modules) {
        super(new EmbeddedRuntime(name, properties, modules));
    }

}
