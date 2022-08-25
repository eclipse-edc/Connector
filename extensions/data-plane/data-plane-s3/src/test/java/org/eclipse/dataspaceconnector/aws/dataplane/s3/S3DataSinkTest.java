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
 *       ZF Friedrichshafen AG - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.dataplane.s3;

import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Executors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.aws.dataplane.s3.TestFunctions.createRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3DataSinkTest {

    private static final String BUCKET_NAME = "bucketName";
    private static final String KEY_NAME = "keyName";
    private static final String ETAG = "eTag";
    private static final int CHUNK_SIZE_BYTES = 50;

    private S3Client s3ClientMock;
    private S3DataSink dataSink;

    private ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestCaptor;

    @BeforeEach
    void setup() {
        s3ClientMock = mock(S3Client.class);
        completeMultipartUploadRequestCaptor = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);

        dataSink = S3DataSink.Builder.newInstance()
            .bucketName(BUCKET_NAME)
            .keyName(KEY_NAME)
            .client(s3ClientMock)
            .requestId(createRequest(S3BucketSchema.TYPE).build().getId())
            .executorService(Executors.newFixedThreadPool(2))
            .monitor(mock(Monitor.class))
            .chunkSizeBytes(CHUNK_SIZE_BYTES)
            .build();

        when(s3ClientMock.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("uploadId").build());
        when(s3ClientMock.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().eTag(ETAG).build());
    }

    @Test
    void transferParts_singlePart_succeeds() {
        var result = dataSink.transferParts(
                List.of(new InputStreamDataSource(KEY_NAME, new ByteArrayInputStream("content smaller than a chunk size".getBytes(UTF_8)))));
        assertThat(result.succeeded()).isTrue();
        verify(s3ClientMock).completeMultipartUpload(completeMultipartUploadRequestCaptor.capture());

        var completeMultipartUploadRequest = completeMultipartUploadRequestCaptor.getValue();
        assertThat(completeMultipartUploadRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(completeMultipartUploadRequest.key()).isEqualTo(KEY_NAME);
        assertThat(completeMultipartUploadRequest.multipartUpload().parts()).hasSize(1);
    }

    @Test
    void transferParts_multiPart_succeeds() {
        var result = dataSink.transferParts(
                List.of(new InputStreamDataSource(KEY_NAME,
                        new ByteArrayInputStream("content bigger than 50 bytes chunk size so that it gets chunked and uploaded as a multipart upload"
                            .getBytes(UTF_8)))));
        assertThat(result.succeeded()).isTrue();
        verify(s3ClientMock).completeMultipartUpload(completeMultipartUploadRequestCaptor.capture());

        var completeMultipartUploadRequest = completeMultipartUploadRequestCaptor.getValue();
        assertThat(completeMultipartUploadRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(completeMultipartUploadRequest.key()).isEqualTo(KEY_NAME);

        assertThat(completeMultipartUploadRequest.multipartUpload().parts()).hasSize(2);
    }

    @Test
    void complete_succeedIfPutObjectSucceeds() {
        when(s3ClientMock.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        var result = dataSink.complete();

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void complete_failsIfPutObjectFails() {
        when(s3ClientMock.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkException.builder().message("an error").build());

        var result = dataSink.complete();

        assertThat(result.failed()).isTrue();
    }
}
