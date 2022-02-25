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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is a wrapper solely used to store objects in an Azure CosmosDB.
 * Some features or requirements of CosmosDB don't fit into an object data model,
 * such as the "partition key", which is required by CosmosDB to achieve a better distribution of read/write load.
 */
public abstract class CosmosDocument<T> {

    @JsonProperty
    private final T wrappedInstance;

    @JsonProperty
    private final String partitionKey;


    protected CosmosDocument(@JsonProperty("wrappedInstance") T wrappedInstance, @JsonProperty("partitionKey") String partitionKey) {
        this.wrappedInstance = wrappedInstance;
        this.partitionKey = partitionKey;
    }

    public static String sanitize(String key) {
        return key.replace(':', '_');
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public T getWrappedInstance() {
        return wrappedInstance;
    }

    public abstract String getId();

}
