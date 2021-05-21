/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectContainerProvisionedResourceTest {

    private ObjectContainerProvisionedResource resource;

    @BeforeEach
    void setUp() {
        resource = ObjectContainerProvisionedResource.Builder.newInstance()
                .containerName("test-container")
                .accountName("test-account")
                .transferProcessId("test-process-id")
                .resourceDefinitionId("test-resdef-id")
                .id("test-id")
                .build();
    }

    @Test
    void createDataDestination() {

        DataAddress dest = resource.createDataDestination();
        assertThat(dest.getType()).isEqualTo(AzureBlobStoreSchema.TYPE);
        assertThat(dest.getKeyName()).isEqualTo("test-container");
        assertThat(dest.getProperties())
                .hasFieldOrPropertyWithValue(AzureBlobStoreSchema.CONTAINER_NAME, "test-container")
                .hasFieldOrPropertyWithValue(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account");
    }

    @Test
    void getResourceName() {
        assertThat(resource.getResourceName()).isEqualTo("test-container");
    }

    @Test
    void verifySerialization() {
        var typeManager = new TypeManager();
        typeManager.registerTypes(ObjectContainerProvisionedResource.class);

        String s = typeManager.writeValueAsString(resource);
        assertThat(s).isNotNull();
        assertThat(s).contains("accountName")
                .contains("containerName");

    }

    @Test
    void verifyDeserialization() {
        final String json = "{\"id\":\"test-id\",\"transferProcessId\":\"test-process-id\",\"resourceDefinitionId\":\"test-resdef-id\",\"error\":false,\"errorMessage\":null,\"accountName\":\"test-account\",\"containerName\":\"test-container\",\"resourceName\":\"test-container\"}";
        var typeManager = new TypeManager();
        typeManager.registerTypes(ObjectContainerProvisionedResource.class);

        var res = typeManager.readValue(json, ObjectContainerProvisionedResource.class);
        assertThat(res).isNotNull();
        assertThat(res.getContainerName()).isEqualTo("test-container");
        assertThat(res.getAccountName()).isEqualTo("test-account");
        assertThat(res).usingRecursiveComparison().isEqualTo(resource);
    }
}