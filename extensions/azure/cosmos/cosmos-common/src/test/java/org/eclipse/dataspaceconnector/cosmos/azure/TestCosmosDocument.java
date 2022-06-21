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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.cosmos.azure;

import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDocument;

import java.util.UUID;

public class TestCosmosDocument extends CosmosDocument<String> {
    private final String id;

    public TestCosmosDocument(String wrappedInstance, String partitionKey) {
        super(wrappedInstance, partitionKey);
        id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }
}
