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

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public interface Runtimes {

    interface ControlPlane {
        String NAME = "controlplane";

        String[] CONTROL_PLANE_MODULES = new String[]{
                ":system-tests:e2e-transfer-test:control-plane"
        };

        String[] VIRTUAL_MODULES = new String[]{
                ":system-tests:management-api:management-api-test-virtual-runtime"
        };

        String[] VIRTUAL_SQL_MODULES = new String[]{
                ":extensions:common:store:sql:cel-store-sql",
                ":extensions:common:store:sql:task-store-sql",
                ":extensions:control-plane:store:sql:participantcontext-store-sql",
                ":extensions:control-plane:store:sql:participantcontext-config-store-sql",
        };

        String[] VIRTUAL_NATS_MODULES = new String[]{
                ":extensions:control-plane:tasks:nats:publisher:negotiation-tasks-publisher-nats",
                ":extensions:control-plane:tasks:nats:publisher:transfer-tasks-publisher-nats",
                ":extensions:control-plane:tasks:nats:subscriber:negotiation-tasks-subscriber-nats",
                ":extensions:control-plane:tasks:nats:subscriber:transfer-tasks-subscriber-nats",
        };

        String[] LEGACY_SIGNALING_MODULES = new String[]{
                ":system-tests:e2e-transfer-test:control-plane",
                ":extensions:control-plane:transfer:transfer-data-plane-signaling"
        };

        String[] EMBEDDED_DP_MODULES = new String[]{
                ":system-tests:e2e-transfer-test:control-plane",
                ":extensions:control-plane:transfer:transfer-data-plane-signaling",
                ":system-tests:e2e-transfer-test:data-plane"
        };

        String[] SQL_MODULES = new String[]{
                ":dist:bom:controlplane-feature-sql-bom",
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("management", () -> URI.create("http://localhost:" + getFreePort() + "/management"))
                .endpoint("signaling", () -> URI.create("http://localhost:" + getFreePort() + "/signaling"))
                .endpoint("control", () -> URI.create("http://localhost:" + getFreePort() + "/control"))
                .endpoint("protocol", () -> URI.create("http://localhost:" + getFreePort() + "/protocol"));

        static Config config(String participantId) {
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.participant.id", participantId);
                    put("edc.transfer.send.retry.limit", "3");
                    put("edc.transfer.send.retry.base-delay.ms", "500");
                    put("edc.transfer.state-machine.iteration-wait-millis", "50");
                    put("edc.negotiation.send.retry.limit", "1");
                    put("edc.negotiation.send.retry.base-delay.ms", "100");
                    put("edc.negotiation.state-machine.iteration-wait-millis", "50");
                    put("edc.data.plane.selector.state-machine.iteration-wait-millis", "100");
                    put("edc.core.retry.retries.max", "1");
                    put("edc.policy.monitor.period", "PT2S");
                }
            });
        }

        static Config dataPlaneSelectorFor(Endpoints endpoints) {
            var controlEndpoint = Objects.requireNonNull(endpoints.getEndpoint("control"));
            return ConfigFactory.fromMap(Map.of(
                    "edc.dpf.selector.url", controlEndpoint.get() + "/v1/dataplanes"
            ));
        }
    }

    interface DataPlane {

        String[] IN_MEM_MODULES = new String[]{
                ":system-tests:e2e-transfer-test:data-plane",
                ":extensions:data-plane-selector:data-plane-selector-client"
        };

        String[] SQL_MODULES = new String[]{
                ":system-tests:e2e-transfer-test:data-plane",
                ":dist:bom:dataplane-feature-sql-bom",
                ":extensions:data-plane-selector:data-plane-selector-client"
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("control", () -> URI.create("http://localhost:" + getFreePort() + "/control"));

        static Config config() {
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                    put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                    put("edc.dataplane.http.sink.partition.size", "1");
                    put("edc.dataplane.send.retry.limit", "1");
                    put("edc.dataplane.state-machine.iteration-wait-millis", "50");
                }
            });
        }

    }

    interface SignalingDataPlane {
        String[] MODULES = new String[]{
                ":system-tests:e2e-transfer-test:signaling-data-plane"
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("default", () -> URI.create("http://localhost:" + getFreePort() + "/api"));

        static Config config() {
            return ConfigFactory.fromMap(Map.of("dataplane.id", UUID.randomUUID().toString()));
        }
    }
}
