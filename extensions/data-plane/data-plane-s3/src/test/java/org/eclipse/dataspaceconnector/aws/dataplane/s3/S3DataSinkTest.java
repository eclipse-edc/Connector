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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
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

    private final Monitor monitor = mock(Monitor.class);
    private final S3Client s3ClientMock = mock(S3Client.class);
    private final DataFlowRequest.Builder request = createRequest(S3BucketSchema.TYPE);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartUploadRequestCaptor =
            ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);

    private final S3DataSink dataSink = S3DataSink.Builder.newInstance()
            .bucketName(BUCKET_NAME)
            .keyName(KEY_NAME)
            .client(s3ClientMock)
            .requestId(request.build().getId())
            .executorService(executor)
            .monitor(monitor)
            .chunkSizeBytes(CHUNK_SIZE_BYTES)
            .build();

    @BeforeEach
    void setup() {
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

        CompleteMultipartUploadRequest completeMultipartUploadRequest = completeMultipartUploadRequestCaptor.getValue();
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

        CompleteMultipartUploadRequest completeMultipartUploadRequest = completeMultipartUploadRequestCaptor.getValue();
        assertThat(completeMultipartUploadRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(completeMultipartUploadRequest.key()).isEqualTo(KEY_NAME);

        assertThat(completeMultipartUploadRequest.multipartUpload().parts()).hasSize(2);
    }

}
