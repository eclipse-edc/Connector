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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.aws.dataplane.s3;

import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.testfixtures.AbstractS3Test;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.SECRET_ACCESS_KEY;
import static org.mockito.Mockito.mock;

@IntegrationTest
public class S3DataPlaneIntegrationTest extends AbstractS3Test {

    private final String sourceBucketName = "source-" + UUID.randomUUID();
    private final String destinationBucketName = "destination-" + UUID.randomUUID();

    @BeforeEach
    void setup() {
        createBucket(sourceBucketName);
        createBucket(destinationBucketName);
    }

    @AfterEach
    void tearDown() {
        deleteBucket(sourceBucketName);
        deleteBucket(destinationBucketName);
    }

    @Test
    void shouldCopyFromSourceToSink() {
        var body = UUID.randomUUID().toString();
        var key = UUID.randomUUID().toString();
        putStringOnBucket(sourceBucketName, key, body);

        var sinkFactory = new S3DataSinkFactory(clientProvider, Executors.newSingleThreadExecutor(), mock(Monitor.class), mock(Vault.class), new TypeManager());
        var sourceFactory = new S3DataSourceFactory(clientProvider);
        var sourceAddress = DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .keyName(key)
                .property(BUCKET_NAME, sourceBucketName)
                .property(S3BucketSchema.REGION, REGION)
                .property(ACCESS_KEY_ID, getCredentials().accessKeyId())
                .property(SECRET_ACCESS_KEY, getCredentials().secretAccessKey())
                .build();

        var destinationAddress = DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .keyName(key)
                .property(BUCKET_NAME, destinationBucketName)
                .property(S3BucketSchema.REGION, REGION)
                .property(ACCESS_KEY_ID, getCredentials().accessKeyId())
                .property(SECRET_ACCESS_KEY, getCredentials().secretAccessKey())
                .build();

        var request = DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(sourceAddress)
                .destinationDataAddress(destinationAddress)
                .build();

        var sink = sinkFactory.createSink(request);
        var source = sourceFactory.createSource(request);

        var transferResult = sink.transfer(source);

        assertThat(transferResult).succeedsWithin(5, SECONDS);
        assertThat(getObject(key)).succeedsWithin(5, SECONDS)
                .extracting(ResponseBytes::response)
                .extracting(GetObjectResponse::contentLength)
                .extracting(Long::intValue)
                .isEqualTo(body.length());
        assertThat(getObject(key + ".complete")).succeedsWithin(5, SECONDS);
    }

    private CompletableFuture<ResponseBytes<GetObjectResponse>> getObject(String key) {
        var getObjectRequest = GetObjectRequest.builder().bucket(destinationBucketName).key(key).build();
        return clientProvider.s3AsyncClient(REGION)
                .getObject(getObjectRequest, new ByteArrayAsyncResponseTransformer<>());
    }

}
