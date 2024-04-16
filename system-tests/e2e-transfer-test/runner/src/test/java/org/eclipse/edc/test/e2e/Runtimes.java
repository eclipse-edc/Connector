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

    }

    interface EmbeddedDataPlane {

    }

    interface Postgres {

    }

    static EdcRuntimeExtension backendService(String name, Map<String, String> configuration) {
        return new EdcRuntimeExtension(
                ":system-tests:e2e-transfer-test:backend-service",
                name,
                configuration
        );
    }
}
