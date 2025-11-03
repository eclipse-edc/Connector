/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi;

import org.eclipse.edc.junit.utils.Endpoints;

import java.net.URI;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.edc.web.spi.configuration.ApiContext.CONTROL;
import static org.eclipse.edc.web.spi.configuration.ApiContext.MANAGEMENT;
import static org.eclipse.edc.web.spi.configuration.ApiContext.PROTOCOL;

public interface Runtimes {

    interface ControlPlane {
        String NAME = "controlplane";

        String[] MODULES = new String[]{
                ":system-tests:management-api:management-api-test-runtime"
        };

        String[] SQL_MODULES = new String[]{
                ":dist:bom:controlplane-feature-sql-bom"
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint(MANAGEMENT, () -> URI.create("http://localhost:" + getFreePort() + "/management"))
                .endpoint(CONTROL, () -> URI.create("http://localhost:" + getFreePort() + "/control"))
                .endpoint(PROTOCOL, () -> URI.create("http://localhost:" + getFreePort() + "/protocol"));
    }
}
