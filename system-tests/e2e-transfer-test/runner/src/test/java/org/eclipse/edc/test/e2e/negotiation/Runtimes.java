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

package org.eclipse.edc.test.e2e.negotiation;

import org.eclipse.edc.connector.controlplane.test.system.utils.ManagementApiClientV4;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public interface Runtimes {


    interface ControlPlane {
        String[] MODULES = new String[]{
                ":system-tests:e2e-transfer-test:control-plane",
                ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client"
        };

        String[] SQL_MODULES = new String[]{
                ":dist:bom:controlplane-feature-sql-bom",
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("management", () -> URI.create("http://localhost:" + getFreePort() + "/management"))
                .endpoint("control", () -> URI.create("http://localhost:" + getFreePort() + "/control"))
                .endpoint("protocol", () -> URI.create("http://localhost:" + getFreePort() + "/protocol"));


        static Config config(String participantId) {
            return ConfigFactory.fromMap(Map.of("edc.participant.id", participantId));
        }

        static ManagementApiClientV4 getApiClient(ComponentRuntimeContext context) {
            var participantId = context.getConfig().getString("edc.participant.id");
            return ManagementApiClientV4.Builder.newInstance().participantId(participantId)
                    .controlPlaneManagement(context.getEndpoint("management"))
                    .controlPlaneProtocol(context.getEndpoint("protocol"))
                    .build();
        }
    }
}
