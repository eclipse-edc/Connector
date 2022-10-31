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

package org.eclipse.dataspaceconnector.aws.s3.provision;

import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class S3ConsumerResourceDefinitionGeneratorTest {

    private final Pattern regexPattern = Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    private S3ConsumerResourceDefinitionGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new S3ConsumerResourceDefinitionGenerator();
    }

    @Test
    void generateWithDestinationTypeNotNull() {

        DataAddress destination = DataAddress.Builder.newInstance().type(S3BucketSchema.TYPE)
                .property(S3BucketSchema.BUCKET_NAME, "test-name")
                .property(S3BucketSchema.REGION, Region.US_EAST_1.id())
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dr = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).processId("process-id").build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.generate(dr, policy);

        assertThat(definition).isInstanceOf(S3BucketResourceDefinition.class);
        var objectDef = (S3BucketResourceDefinition) definition;
        assertThat(objectDef.getBucketName()).isEqualTo("process-id"); //  ?? S3ConsumerResourceDefinitionGenerator -> line 35: bucketName(dataRequest.getProcessId())
        assertThat(objectDef.getRegionId()).isEqualTo(Region.US_EAST_1.id());
        assertThat(objectDef.getId()).matches(regexPattern);
    }

    @Test
    void canGenerate() {

        DataAddress destination = DataAddress.Builder.newInstance().type(S3BucketSchema.TYPE)
                .property(S3BucketSchema.BUCKET_NAME, "test-name")
                .property(S3BucketSchema.REGION, Region.US_EAST_1.id())
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dr = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).processId("process-id").build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.canGenerate(dr, policy);

        assertThat(definition).isTrue();
    }

    @Test
    void canGenerateIsNotTypeS3BucketSchema() {
        DataAddress destination = DataAddress.Builder.newInstance().type("aNonS3BucketSchema")
                .property(S3BucketSchema.BUCKET_NAME, "test-name")
                .property(S3BucketSchema.REGION, Region.US_EAST_1.id())
                .build();
        var asset = Asset.Builder.newInstance().build();
        var dataRequest = DataRequest.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();
        var policy = Policy.Builder.newInstance().build();

        var definition = generator.canGenerate(dataRequest, policy);
        assertThat(definition).isFalse();
    }

}
