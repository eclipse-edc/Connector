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

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public interface Runtimes {

    interface ControlPlane {

        String[] MODULES = new String[]{
                ":system-tests:e2e-transfer-test:control-plane"
        };

        String[] SIGNALING_MODULES = new String[]{
                ":system-tests:e2e-transfer-test:control-plane",
                ":data-protocols:data-plane-signaling"
        };

        String[] EMBEDDED_DP_MODULES = new String[]{
                ":system-tests:e2e-transfer-test:control-plane",
                ":system-tests:e2e-transfer-test:data-plane"
        };

        String[] SQL_MODULES = new String[]{
                ":dist:bom:controlplane-feature-sql-bom",
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("management", () -> URI.create("http://localhost:" + getFreePort() + "/management"))
                .endpoint("control", () -> URI.create("http://localhost:" + getFreePort() + "/control"))
                .endpoint("protocol", () -> URI.create("http://localhost:" + getFreePort() + "/protocol"));

        static Config config(String participantId) {
            return ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("edc.participant.id", participantId);
                    put("edc.transfer.send.retry.limit", "1");
                    put("edc.transfer.send.retry.base-delay.ms", "100");
                    put("edc.negotiation.consumer.send.retry.limit", "1");
                    put("edc.negotiation.provider.send.retry.limit", "1");
                    put("edc.negotiation.consumer.send.retry.base-delay.ms", "100");
                    put("edc.negotiation.provider.send.retry.base-delay.ms", "100");
                    put("edc.negotiation.consumer.state-machine.iteration-wait-millis", "50");
                    put("edc.negotiation.provider.state-machine.iteration-wait-millis", "50");
                    put("edc.transfer.state-machine.iteration-wait-millis", "50");
                    put("edc.data.plane.selector.state-machine.iteration-wait-millis", "100");
                    put("edc.core.retry.retries.max", "1");
                }
            });
        }

        static Config dataPlaneSelectorFor(Endpoints endpoints) {
            var controlEndpoint = Objects.requireNonNull(endpoints.getEndpoint("control"));
            return ConfigFactory.fromMap(Map.of(
                    "edc.dpf.selector.url", controlEndpoint.get() + "/v1/dataplanes"
            ));
        }

        static Config controlPlaneEndpointOf(Endpoints endpoints) {
            var controlEndpoint = Objects.requireNonNull(endpoints.getEndpoint("control"));
            return ConfigFactory.fromMap(Map.of(
                    "signaling.dataplane.controlplane.endpoint", controlEndpoint.get().toString()
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
        String[] MODULES = new String[] {
                ":system-tests:e2e-transfer-test:signaling-data-plane"
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("default", () -> URI.create("http://localhost:" + getFreePort() + "/api"));

        static Config config() {
            return ConfigFactory.fromMap(emptyMap());
        }
    }
}
