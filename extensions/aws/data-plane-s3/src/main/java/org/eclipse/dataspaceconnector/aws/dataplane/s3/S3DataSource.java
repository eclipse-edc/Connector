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

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.io.InputStream;
import java.util.stream.Stream;

class S3DataSource implements DataSource {

    private String bucketName;
    private String keyName;
    private S3Client client;

    private S3DataSource() { }

    @Override
    public Stream<Part> openPartStream() {
        return Stream.of(new S3Part(client, keyName, bucketName));
    }

    private static class S3Part implements Part {
        private final S3Client client;
        private final String keyName;
        private final String bucketName;

        public S3Part(S3Client client, String keyName, String bucketName) {
            this.client = client;
            this.keyName = keyName;
            this.bucketName = bucketName;
        }

        @Override
        public String name() {
            return keyName;
        }

        @Override
        public long size() {
            var request = HeadObjectRequest.builder().key(keyName).bucket(bucketName).build();
            return client.headObject(request).contentLength();
        }

        @Override
        public InputStream openStream() {
            var request = GetObjectRequest.builder().key(keyName).bucket(bucketName).build();
            return client.getObject(request);
        }
    }

    public static class Builder {
        private final S3DataSource source;

        private Builder() {
            source = new S3DataSource();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder bucketName(String bucketName) {
            source.bucketName = bucketName;
            return this;
        }

        public Builder keyName(String keyName) {
            source.keyName = keyName;
            return this;
        }

        public Builder client(S3Client client) {
            source.client = client;
            return this;
        }

        public S3DataSource build() {
            return source;
        }
    }
}
