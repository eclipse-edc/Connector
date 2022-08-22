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
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.junit.extensions;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A JUnit extension to be used for tests that need to attach the OpenTelemetry java agent.
 */
public class OpenTelemetryExtension implements BeforeTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        checkForAgent();
    }

    private void checkForAgent() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        assertThat(runtimeMxBean.getInputArguments())
                .withFailMessage("OpenTelemetry Agent JAR should be attached to run the tests with the -javaagent JVM flag.")
                .anyMatch(arg -> arg.startsWith("-javaagent") && arg.contains("opentelemetry-javaagent.jar"));
    }
}
