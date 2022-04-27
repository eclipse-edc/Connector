/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;


import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Pulls data from an Azure Storage blob source.
 */
public class AzureStorageDataSource implements DataSource {
    private String accountName;
    private String containerName;
    private String sharedKey;
    private String blobName;
    private String requestId;
    private RetryPolicy<Object> retryPolicy;
    private BlobAdapterFactory blobAdapterFactory;
    private Monitor monitor;

    @Override
    public Stream<Part> openPartStream() {
        return Stream.of(getPart());
    }

    private AzureStoragePart getPart() {
        try {
            var adapter = blobAdapterFactory.getBlobAdapter(accountName, containerName, blobName, sharedKey);
            return new AzureStoragePart(adapter);
        } catch (Exception e) {
            monitor.severe(format("Error accessing blob %s on account %s", blobName, accountName), e);
            throw new EdcException(e);
        }
    }

    private AzureStorageDataSource() {
    }

    public static class Builder {
        private AzureStorageDataSource dataSource;

        private Builder() {
            dataSource = new AzureStorageDataSource();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accountName(String accountName) {
            dataSource.accountName = accountName;
            return this;
        }

        public Builder containerName(String containerName) {
            dataSource.containerName = containerName;
            return this;
        }

        public Builder sharedKey(String sharedKey) {
            dataSource.sharedKey = sharedKey;
            return this;
        }

        public Builder blobName(String blobName) {
            dataSource.blobName = blobName;
            return this;
        }

        public Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public Builder retryPolicy(RetryPolicy<Object> retryPolicy) {
            dataSource.retryPolicy = retryPolicy;
            return this;
        }

        public Builder blobAdapterFactory(BlobAdapterFactory blobAdapterFactory) {
            dataSource.blobAdapterFactory = blobAdapterFactory;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public AzureStorageDataSource build() {
            Objects.requireNonNull(dataSource.accountName, "accountName");
            Objects.requireNonNull(dataSource.containerName, "containerName");
            Objects.requireNonNull(dataSource.sharedKey, "sharedKey");
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.blobAdapterFactory, "blobAdapterFactory");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.retryPolicy, "retryPolicy");
            return dataSource;
        }
    }

    private static class AzureStoragePart implements Part {
        private final BlobAdapter adapter;

        AzureStoragePart(BlobAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public String name() {
            return adapter.getBlobName();
        }

        @Override
        public long size() {
            return adapter.getBlobSize();
        }

        @Override
        public InputStream openStream() {
            return adapter.openInputStream();
        }
    }

}
