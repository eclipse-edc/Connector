/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.edc.connector.provision.gcp;

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.gcp.storage.GcsStoreSchema;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class GcsConsumerResourceDefinitionGeneratorTest {

    private final Pattern regexPattern = Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    private GcsConsumerResourceDefinitionGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new GcsConsumerResourceDefinitionGenerator();
    }

    @Test
    void generate() {
        DataAddress destination = DataAddress.Builder.newInstance().type(GcsStoreSchema.TYPE)
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
        assertThat(objectDef.getId()).matches(regexPattern);
    }

    @Test
    void canGenerate() {
        DataAddress destination = DataAddress.Builder.newInstance().type(GcsStoreSchema.TYPE)
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
        DataAddress destination = DataAddress.Builder.newInstance().type("aNonGcsStream")
                .property(GcsStoreSchema.STORAGE_CLASS, "test-storage-class")
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.canGenerate(dataRequest, policy);
        assertThat(definition).isFalse();
    }

}
