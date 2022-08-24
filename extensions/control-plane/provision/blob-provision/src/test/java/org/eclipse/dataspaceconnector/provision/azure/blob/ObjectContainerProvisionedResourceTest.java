/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.provision.azure.blob;

import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
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
                .resourceName("test-container")
                .id("test-id")
                .build();
    }

    @Test
    void createDataDestination() {

        DataAddress dest = resource.getDataAddress();
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
        assertThat(s).isNotNull()
                .contains("accountName")
                .contains("containerName");

    }

    @Test
    void verifyDeserialization() {
        final String json = "{\"id\":\"test-id\",\"edctype\":\"dataspaceconnector:objectcontainerprovisionedresource\", " +
                "\"transferProcessId\":\"test-process-id\",\"resourceDefinitionId\":\"test-resdef-id\"," +
                "\"accountName\":\"test-account\",\"containerName\":\"test-container\",\"resourceName\":\"test-container\"}";
        var typeManager = new TypeManager();
        typeManager.registerTypes(ObjectContainerProvisionedResource.class);

        var res = typeManager.readValue(json, ObjectContainerProvisionedResource.class);
        assertThat(res).isNotNull();
        assertThat(res.getContainerName()).isEqualTo("test-container");
        assertThat(res.getAccountName()).isEqualTo("test-account");
        assertThat(res).usingRecursiveComparison().isEqualTo(resource);
    }
}
