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

    static EdcRuntimeExtension controlPlane(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(name, configuration,
                ":system-tests:e2e-transfer-test:control-plane",
                ":extensions:control-plane:transfer:transfer-data-plane",
                ":extensions:data-plane:data-plane-client"
        );
    }

    static EdcRuntimeExtension controlPlaneSignaling(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(name, configuration,
                ":system-tests:e2e-transfer-test:control-plane",
                ":extensions:control-plane:transfer:transfer-data-plane-signaling",
                ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client"
        );
    }

    static EdcRuntimeExtension dataPlane(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(name, configuration,
                ":system-tests:e2e-transfer-test:data-plane",
                ":extensions:data-plane:data-plane-public-api"
        );
    }

    static EdcRuntimeExtension backendService(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(name, configuration,
                ":system-tests:e2e-transfer-test:backend-service"
        );
    }

    static EdcRuntimeExtension postgresControlPlane(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(name, configuration,
                ":system-tests:e2e-transfer-test:control-plane",
                ":extensions:control-plane:transfer:transfer-data-plane",
                ":extensions:data-plane:data-plane-client",
                ":extensions:control-plane:store:sql:control-plane-sql",
                ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
                ":extensions:common:transaction:transaction-local",
                ":extensions:common:api:management-api-configuration",
                ":extensions:policy-monitor:store:sql:policy-monitor-store-sql"
        );
    }

    static EdcRuntimeExtension postgresDataPlane(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(name, configuration,
                ":system-tests:e2e-transfer-test:data-plane",
                ":extensions:data-plane:data-plane-public-api",
                ":extensions:data-plane:store:sql:data-plane-store-sql",
                ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
                ":extensions:common:transaction:transaction-local"
        );
    }
}
