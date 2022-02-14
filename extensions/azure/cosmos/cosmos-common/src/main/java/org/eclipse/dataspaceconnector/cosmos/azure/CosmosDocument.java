package org.eclipse.dataspaceconnector.cosmos.azure;

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
