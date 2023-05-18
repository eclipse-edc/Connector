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

package org.eclipse.edc.connector.provision.azure.blob;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;

class ObjectContainerProvisionedResourceTest {

    private ObjectContainerProvisionedResource resource;
    private final TypeManager typeManager = new TypeManager();

    @BeforeEach
    void setUp() {
        typeManager.registerTypes(ObjectContainerProvisionedResource.class);

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
        var dest = resource.getDataAddress();

        assertThat(dest.getType()).isEqualTo(AzureBlobStoreSchema.TYPE);
        assertThat(dest.getKeyName()).isEqualTo("test-container");
        assertThat(dest.getProperty(CONTAINER_NAME)).isEqualTo("test-container");
        assertThat(dest.getProperty(ACCOUNT_NAME)).isEqualTo("test-account");
    }

    @Test
    void getResourceName() {
        assertThat(resource.getResourceName()).isEqualTo("test-container");
    }

    @Test
    void verifySerialization() {
        var json = typeManager.writeValueAsString(resource);

        assertThat(json).isNotNull()
                .contains("accountName")
                .contains("containerName");
    }

    @Test
    void verifyDeserialization() {
        var serialized = Map.of(
                "id", "test-id",
                "edctype", "dataspaceconnector:objectcontainerprovisionedresource",
                "transferProcessId", "test-process-id",
                "resourceDefinitionId", "test-resdef-id",
                "accountName", "test-account",
                "containerName", "test-container",
                "resourceName", "test-container"
        );

        var res = typeManager.readValue(typeManager.writeValueAsBytes(serialized), ObjectContainerProvisionedResource.class);

        assertThat(res).isNotNull();
        assertThat(res.getContainerName()).isEqualTo("test-container");
        assertThat(res.getAccountName()).isEqualTo("test-account");
        assertThat(res).usingRecursiveComparison().isEqualTo(resource);
    }
}
