/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.cosmos;

public class TestDocument extends LeaseableCosmosDocument<String> {
    protected TestDocument(String wrappedInstance, String partitionKey) {
        super(wrappedInstance, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance();
    }
}
