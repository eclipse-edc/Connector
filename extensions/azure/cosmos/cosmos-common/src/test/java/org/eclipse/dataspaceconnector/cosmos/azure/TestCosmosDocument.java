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
