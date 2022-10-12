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

import org.eclipse.dataspaceconnector.dataplane.common.sink.ParallelSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;

class S3DataSink extends ParallelSink {

    private S3Client client;
    private String bucketName;
    private String keyName;
    private int chunkSize;

    private S3DataSink() {}

    @Override
    protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
        for (var part : parts) {
            try (var input = part.openStream()) {

                var partNumber = 1;
                var completedParts = new ArrayList<CompletedPart>();

                var uploadId = client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(keyName)
                        .build()).uploadId();

                while (true) {
                    var bytesChunk = input.readNBytes(chunkSize);

                    if (bytesChunk.length < 1) {
                        break;
                    }

                    completedParts.add(CompletedPart.builder().partNumber(partNumber)
                            .eTag(client.uploadPart(UploadPartRequest.builder()
                                .bucket(bucketName)
                                .key(keyName)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build(), RequestBody.fromByteBuffer(ByteBuffer.wrap(bytesChunk))).eTag()).build());
                    partNumber++;
                }

                client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(keyName)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                        .build());

            } catch (Exception e) {
                return uploadFailure(e, keyName);
            }
        }

        return StatusResult.success();
    }

    @Override
    protected StatusResult<Void> complete() {
        var completeKeyName = keyName + ".complete";
        var request = PutObjectRequest.builder().bucket(bucketName).key(completeKeyName).build();
        try {
            client.putObject(request, RequestBody.empty());
            return super.complete();
        } catch (Exception e) {
            return uploadFailure(e, completeKeyName);
        }

    }

    @NotNull
    private StatusResult<Void> uploadFailure(Exception e, String keyName) {
        var message = format("Error writing the %s object on the %s bucket: %s", keyName, bucketName, e.getMessage());
        monitor.severe(message, e);
        return StatusResult.failure(FATAL_ERROR, message);
    }

    public static class Builder extends ParallelSink.Builder<Builder, S3DataSink> {

        private Builder() {
            super(new S3DataSink());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder client(S3Client client) {
            sink.client = client;
            return this;
        }

        public Builder bucketName(String bucketName) {
            sink.bucketName = bucketName;
            return this;
        }

        public Builder keyName(String keyName) {
            sink.keyName = keyName;
            return this;
        }

        public Builder chunkSizeBytes(int chunkSize) {
            sink.chunkSize = chunkSize;
            return this;
        }

        @Override
        protected void validate() {}
    }
}
