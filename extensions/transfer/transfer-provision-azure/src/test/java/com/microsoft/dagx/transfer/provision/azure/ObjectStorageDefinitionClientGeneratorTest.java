/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataCatalog;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageDefinitionClientGeneratorTest {

    private ObjectStorageDefinitionClientGenerator generator;
    private final Pattern regexPattern = Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");

    @BeforeEach
    void setUp() {
        generator = new ObjectStorageDefinitionClientGenerator();
    }

    @Test
    void generate() {
        DataAddress destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, "test-container")
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                .build();
        var entry = DataEntry.Builder.newInstance().catalog(GenericDataCatalog.Builder.newInstance().build())
                .build();
        var dr = DataRequest.Builder.newInstance().dataDestination(destination)
                .dataEntry(entry).build();
        var tp = TransferProcess.Builder.newInstance().dataRequest(dr).id(randomUUID().toString()).build();


        ResourceDefinition def = generator.generate(tp);

        assertThat(def).isInstanceOf(ObjectStorageResourceDefinition.class);
        var objectDef = (ObjectStorageResourceDefinition) def;
        assertThat(objectDef.getAccountName()).isEqualTo("test-account");
        assertThat(objectDef.getContainerName()).isEqualTo("test-container");
        assertThat(objectDef.getId()).matches(regexPattern);
    }

    @Test
    void generate_containerIsNull() {
        DataAddress destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, null)
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                .build();
        var entry = DataEntry.Builder.newInstance().catalog(GenericDataCatalog.Builder.newInstance().build())
                .build();
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(destination)
                .dataEntry(entry).build();
        var tp = TransferProcess.Builder.newInstance().dataRequest(dataRequest).id(randomUUID().toString()).build();


        ResourceDefinition def = generator.generate(tp);
        assertThat(def).isNotNull();
        assertThat(((ObjectStorageResourceDefinition) def).getContainerName()).matches(
                regexPattern);
    }
}