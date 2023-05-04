/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.provision.gcp;

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.gcp.storage.GcsStoreSchema;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class GcsConsumerResourceDefinitionGeneratorTest {

    private GcsConsumerResourceDefinitionGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new GcsConsumerResourceDefinitionGenerator();
    }

    @Test
    void generate() {
        var destination = DataAddress.Builder.newInstance().type(GcsStoreSchema.TYPE)
                .property(GcsStoreSchema.LOCATION, "test-location")
                .property(GcsStoreSchema.STORAGE_CLASS, "test-storage-class")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dr = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.generate(dr, policy);

        assertThat(definition).isInstanceOf(GcsResourceDefinition.class);
        var objectDef = (GcsResourceDefinition) definition;
        assertThat(objectDef.getLocation()).isEqualTo("test-location");
        assertThat(objectDef.getStorageClass()).isEqualTo("test-storage-class");
        assertThat(objectDef.getId()).satisfies(UUID::fromString);
    }

    @Test
    void generate_noDataRequestAsParameter() {
        var policy = Policy.Builder.newInstance().build();
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> generator.generate(null, policy));
    }

    @Test
    void canGenerate() {
        var destination = DataAddress.Builder.newInstance().type(GcsStoreSchema.TYPE)
                .property(GcsStoreSchema.STORAGE_CLASS, "test-storage-class")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.canGenerate(dataRequest, policy);
        assertThat(definition).isTrue();
    }

    @Test
    void canGenerate_isNotTypeGcsStream() {
        var destination = DataAddress.Builder.newInstance().type("aNonGcsStream")
                .property(GcsStoreSchema.STORAGE_CLASS, "test-storage-class")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.canGenerate(dataRequest, policy);
        assertThat(definition).isFalse();
    }

}
