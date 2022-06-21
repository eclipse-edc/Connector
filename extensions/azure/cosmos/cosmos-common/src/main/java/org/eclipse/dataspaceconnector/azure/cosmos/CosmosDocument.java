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
 * Some features or requirements of CosmosDB don't fit into the EDCs object data model,
 * such as the "partition key", which is required by CosmosDB to achieve a better distribution of read/write load.
 * <p>
 * Considerations when choosing a partition key:
 * Generally it's advisable to adhere to the <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/partitioning-overview#choose-partitionkey">official documentation</a>.
 * <p><p>
 * Note that it is not possible to execute stored procedures across logical partitions, which makes using the item ID impossible
 * when an SP is used to query multiple items. In those cases it is recommended to use a static partition key, like a configuration value.
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
