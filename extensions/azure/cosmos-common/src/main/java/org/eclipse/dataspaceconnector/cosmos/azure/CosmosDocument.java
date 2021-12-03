package org.eclipse.dataspaceconnector.cosmos.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * This is a wrapper solely used to store objects in an Azure CosmosDB.
 * Some features or requirements of CosmosDB don't fit into an object data model,
 * such as the "partition key", which is required by CosmosDB to achieve a better distribution of read/write load.
 */
public abstract class CosmosDocument<T> {

    @JsonUnwrapped
    private T wrappedInstance;

    @JsonProperty
    private String partitionKey;

    protected CosmosDocument() {
        //Jackson does not yet support the combination of @JsonUnwrapped and a @JsonProperty annotation in a constructor
    }

    protected CosmosDocument(T wrappedInstance, String partitionKey) {
        this.wrappedInstance = wrappedInstance;
        this.partitionKey = partitionKey;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public T getWrappedInstance() {
        return wrappedInstance;
    }

    public abstract String getId();
}
