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
 *
 */

package org.eclipse.dataspaceconnector.system.tests.utils;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import io.gatling.javaapi.core.Simulation;

/**
 * Utilities for Gatling tests.
 */
public class GatlingUtils {

    /**
     * Runs a Gatling simulation.
     *
     * @param simulation  Gatling simulation class. Must have a public no-args constructor.
     * @param description Description to be included in HTML report banner.
     * @throws AssertionError if Gatling assertions fails.
     */
    public static void runGatling(Class<? extends Simulation> simulation, String description) {
        var props = new GatlingPropertiesBuilder();
        props.simulationClass(simulation.getCanonicalName());
        props.resultsDirectory("build/reports/gatling");
        props.runDescription(description);

        var statusCode = Gatling.fromMap(props.build());

        if (statusCode != 0) {
            throw new AssertionError("Gatling Simulation failed");
        }
    }
}
