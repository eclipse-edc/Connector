/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.store;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.store.cosmos.DataPlaneInstanceDocument;

import java.util.UUID;

public class TestFunctions {
    public static final String DATA_PLANE_URL = "https://test.edc.org";

    public static final int DATA_PLANE_TURN_COUNT = 0;

    public static DataPlaneInstance generateDataPlaneInstance() {
        return DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url(DATA_PLANE_URL)
                .turnCount(DATA_PLANE_TURN_COUNT)
                .property("somekey-1", "someval-1")
                .property("somekey-2", "someval-2")
                .build();
    }

    public static DataPlaneInstanceDocument generateDocument(String partitionKey) {
        return new DataPlaneInstanceDocument(generateDataPlaneInstance(), partitionKey);
    }
}
