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

import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;

import java.util.Map;

public interface Runtimes {

    interface InMemory {

        static EdcRuntimeExtension controlPlane(String name, Map<String, String> configuration) {
            return new EdcRuntimeExtension(name, configuration,
                    ":system-tests:e2e-transfer-test:control-plane",
                    ":core:common:edr-store-core",
                    ":extensions:control-plane:transfer:transfer-data-plane-signaling",
                    ":extensions:control-plane:api:management-api:edr-cache-api",
                    ":extensions:control-plane:edr:edr-store-receiver",
                    ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client",
                    ":extensions:control-plane:callback:callback-event-dispatcher",
                    ":extensions:control-plane:callback:callback-http-dispatcher"
            );
        }

        static EdcRuntimeExtension controlPlaneEmbeddedDataPlane(String name, Map<String, String> configuration) {
            return new EdcRuntimeExtension(name, configuration,
                    ":system-tests:e2e-transfer-test:control-plane",
                    ":system-tests:e2e-transfer-test:data-plane",
                    ":extensions:control-plane:transfer:transfer-data-plane-signaling",
                    ":extensions:data-plane:data-plane-self-registration",
                    ":extensions:data-plane:data-plane-public-api-v2"
            );
        }

        static EdcRuntimeExtension dataPlane(String name, Map<String, String> configuration) {
            return new EdcRuntimeExtension(name, configuration,
                    ":system-tests:e2e-transfer-test:data-plane",
                    ":extensions:data-plane:data-plane-public-api-v2",
                    ":extensions:data-plane-selector:data-plane-selector-client"
            );
        }
    }

    interface Postgres {

        static EdcRuntimeExtension controlPlane(String name, Map<String, String> configuration) {
            return new EdcRuntimeExtension(name, configuration,
                    ":system-tests:e2e-transfer-test:control-plane",
                    ":core:common:edr-store-core",
                    ":extensions:common:store:sql:edr-index-sql",
                    ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
                    ":extensions:common:transaction:transaction-local",
                    ":extensions:control-plane:transfer:transfer-data-plane-signaling",
                    ":extensions:control-plane:api:management-api:edr-cache-api",
                    ":extensions:control-plane:edr:edr-store-receiver",
                    ":extensions:control-plane:store:sql:control-plane-sql",
                    ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client",
                    ":extensions:control-plane:callback:callback-event-dispatcher",
                    ":extensions:control-plane:callback:callback-http-dispatcher"
            );
        }

        static EdcRuntimeExtension dataPlane(String name, Map<String, String> configuration) {
            return new EdcRuntimeExtension(name, configuration,
                    ":system-tests:e2e-transfer-test:data-plane",
                    ":extensions:data-plane:store:sql:data-plane-store-sql",
                    ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
                    ":extensions:common:transaction:transaction-local",
                    ":extensions:data-plane:data-plane-public-api-v2",
                    ":extensions:data-plane-selector:data-plane-selector-client"
            );
        }

    }

    static EdcRuntimeExtension backendService(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(name, configuration,
                ":system-tests:e2e-transfer-test:backend-service"
        );
    }
}
