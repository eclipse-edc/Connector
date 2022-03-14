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

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.testfixtures.AbstractS3Test;
import org.eclipse.dataspaceconnector.aws.testfixtures.TestS3ClientProvider;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.UUID;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.SECRET_ACCESS_KEY;
import static org.mockito.Mockito.mock;

@IntegrationTest
public class S3DataPlaneIntegrationTest extends AbstractS3Test {

    static Faker faker = new Faker();

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
        putStringOnBucket(this.sourceBucketName, "key", faker.lorem().sentence());

        var s3ClientProvider = new TestS3ClientProvider(getCredentials(), S3_ENDPOINT);

        var sinkFactory = new S3DataSinkFactory(s3ClientProvider, Executors.newSingleThreadExecutor(), mock(Monitor.class), mock(AwsCredentialsProvider.class));
        var sourceFactory = new S3DataSourceFactory(s3ClientProvider, mock(AwsCredentialsProvider.class));
        var sourceAddress = DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .keyName("key")
                .property(BUCKET_NAME, sourceBucketName)
                .property(S3BucketSchema.REGION, REGION)
                .property(ACCESS_KEY_ID, getCredentials().accessKeyId())
                .property(SECRET_ACCESS_KEY, getCredentials().secretAccessKey())
                .build();

        var destinationAddress = DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .keyName("key")
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

        assertThat(sink.transfer(source)).succeedsWithin(5, SECONDS);
        var getObjectRequest = GetObjectRequest.builder().bucket(destinationBucketName).key("key").build();
        var response = client.getObject(getObjectRequest, new ByteArrayAsyncResponseTransformer<>());
        assertThat(response).succeedsWithin(10, SECONDS);
    }

}
