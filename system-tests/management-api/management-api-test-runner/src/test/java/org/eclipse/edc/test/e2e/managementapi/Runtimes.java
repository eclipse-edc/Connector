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
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.Map;

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

        String[] VIRTUAL_MODULES = new String[]{
                ":system-tests:management-api:management-api-test-virtual-runtime",
                ":extensions:control-plane:api:management-api-v5",
                ":extensions:common:api:management-api-authorization",
                ":extensions:common:api:management-api-oauth2-authentication",
                ":core:common:cel-core",
        };

        String[] SQL_MODULES = new String[]{
                ":dist:bom:controlplane-feature-sql-bom"
        };

        String[] VIRTUAL_SQL_MODULES = new String[]{
                ":extensions:common:store:sql:cel-store-sql",
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint(MANAGEMENT, () -> URI.create("http://localhost:" + getFreePort() + "/management"))
                .endpoint(CONTROL, () -> URI.create("http://localhost:" + getFreePort() + "/control"))
                .endpoint(PROTOCOL, () -> URI.create("http://localhost:" + getFreePort() + "/protocol"));


        static Config config() {
            return ConfigFactory.fromMap(Map.of(
                    "edc.participant.id", "anonymous")
            );
        }
    }
}
