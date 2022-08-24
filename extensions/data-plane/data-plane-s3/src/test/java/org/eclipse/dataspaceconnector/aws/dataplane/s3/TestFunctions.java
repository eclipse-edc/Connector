/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.dataplane.s3;

import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.UUID;

public class TestFunctions {

    public static final String VALID_REGION = "validRegion";
    public static final String VALID_BUCKET_NAME = "validBucketName";
    public static final String VALID_ACCESS_KEY_ID = "validAccessKeyId";
    public static final String VALID_SECRET_ACCESS_KEY = "validSecretAccessKey";

    public static DataAddress s3DataAddressWithCredentials() {
        return DataAddress.Builder.newInstance()
            .type(S3BucketSchema.TYPE)
            .keyName("aKey")
            .property(S3BucketSchema.BUCKET_NAME, VALID_BUCKET_NAME)
            .property(S3BucketSchema.REGION, VALID_REGION)
            .property(S3BucketSchema.ACCESS_KEY_ID, VALID_ACCESS_KEY_ID)
            .property(S3BucketSchema.SECRET_ACCESS_KEY, VALID_SECRET_ACCESS_KEY)
            .build();
    }

    public static DataAddress s3DataAddressWithoutCredentials() {
        return DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .keyName("aKey")
                .property(S3BucketSchema.BUCKET_NAME, VALID_BUCKET_NAME)
                .property(S3BucketSchema.REGION, VALID_REGION)
                .build();
    }

    public static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .processId(UUID.randomUUID().toString())
            .sourceDataAddress(createDataAddress(type).build())
            .destinationDataAddress(createDataAddress(type).build());
    }

    public static DataAddress.Builder createDataAddress(String type) {
        return DataAddress.Builder.newInstance().type(type);
    }

}
