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
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageConsumerResourceDefinitionGeneratorTest {

    private final Pattern regexPattern = Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    private ObjectStorageConsumerResourceDefinitionGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ObjectStorageConsumerResourceDefinitionGenerator();
    }

    @Test
    void generate_withContainerName() {
        DataAddress destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, "test-container")
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dr = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.generate(dr, policy);

        assertThat(definition).isInstanceOf(ObjectStorageResourceDefinition.class);
        var objectDef = (ObjectStorageResourceDefinition) definition;
        assertThat(objectDef.getAccountName()).isEqualTo("test-account");
        assertThat(objectDef.getContainerName()).isEqualTo("test-container");
        assertThat(objectDef.getId()).matches(regexPattern);
    }

    @Test
    void generate_withoutContainerName() {
        DataAddress destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dr = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.generate(dr, policy);

        assertThat(definition).isInstanceOf(ObjectStorageResourceDefinition.class);
        var objectDef = (ObjectStorageResourceDefinition) definition;
        assertThat(objectDef.getAccountName()).isEqualTo("test-account");
        assertThat(objectDef.getContainerName()).matches(regexPattern);
        assertThat(objectDef.getId()).matches(regexPattern);
    }


    @Test
    void canGenerate() {
        DataAddress destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.canGenerate(dataRequest, policy);
        assertThat(definition).isTrue();
    }

    @Test
    void canGenerate_isNotTypeAzureBlobStoreSchema() {
        DataAddress destination = DataAddress.Builder.newInstance().type("aNonGoogleCloudStorage")
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.canGenerate(dataRequest, policy);
        assertThat(definition).isFalse();
    }

}