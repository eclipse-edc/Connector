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

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.azure.storage.pipeline.TestFunctions.sharedAccessSignatureMatcher;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureDataSourceToDataSinkTest {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Monitor monitor = mock(Monitor.class);
    private final FakeBlobAdapter fakeSource = new FakeBlobAdapter();
    private final FakeBlobAdapter fakeSink = new FakeBlobAdapter();
    private final FakeBlobAdapter fakeCompletionMarker = new FakeBlobAdapter();
    private final String sourceAccountName = AzureStorageTestFixtures.createAccountName();
    private final String sourceContainerName = AzureStorageTestFixtures.createContainerName();
    private final String sourceSharedKey = AzureStorageTestFixtures.createSharedKey();
    private final String sinkAccountName = AzureStorageTestFixtures.createAccountName();
    private final String sinkContainerName = AzureStorageTestFixtures.createContainerName();
    private final String sinkSharedAccessSignature = AzureStorageTestFixtures.createSharedAccessSignature();
    private final String requestId = UUID.randomUUID().toString();

    /**
     * Verifies a sink is able to pull data from the source without exceptions if both endpoints are functioning.
     */
    @Test
    void transfer_success() {
        var fakeSourceFactory = mock(BlobStoreApi.class);
        when(fakeSourceFactory.getBlobAdapter(
                sourceAccountName,
                sourceContainerName,
                fakeSource.name,
                sourceSharedKey
        )).thenReturn(fakeSource);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(sourceAccountName)
                .containerName(sourceContainerName)
                .sharedKey(sourceSharedKey)
                .blobName(fakeSource.name)
                .requestId(requestId)
                .retryPolicy(RetryPolicy.ofDefaults())
                .blobStoreApi(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var fakeSinkFactory = mock(BlobStoreApi.class);
        when(fakeSinkFactory.getBlobAdapter(
                eq(sinkAccountName),
                eq(sinkContainerName),
                eq(fakeSource.name),
                sharedAccessSignatureMatcher(sinkSharedAccessSignature)
        )).thenReturn(fakeSink);
        when(fakeSinkFactory.getBlobAdapter(
                eq(sinkAccountName),
                eq(sinkContainerName),
                argThat(name -> name.endsWith(".complete")),
                sharedAccessSignatureMatcher(sinkSharedAccessSignature)
        )).thenReturn(fakeCompletionMarker);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedAccessSignature(sinkSharedAccessSignature)
                .requestId(requestId)
                .blobStoreApi(fakeSinkFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        assertThat(fakeSink.out.toString()).isEqualTo(fakeSource.content);
    }

    /**
     * Verifies an exception thrown by the source endpoint is handled correctly.
     */
    @Test
    void transfer_WhenSourceFails_fails() throws Exception {

        // simulate source error
        var blobAdapter = mock(BlobAdapter.class);
        when(blobAdapter.getBlobName()).thenReturn(fakeSource.name);
        String errorMessage = "Test error message";
        when(blobAdapter.openInputStream()).thenThrow(new RuntimeException(errorMessage));
        var fakeSourceFactory = mock(BlobStoreApi.class);
        when(fakeSourceFactory.getBlobAdapter(
                sourceAccountName,
                sourceContainerName,
                fakeSource.name,
                sourceSharedKey
        )).thenReturn(blobAdapter);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(sourceAccountName)
                .containerName(sourceContainerName)
                .sharedKey(sourceSharedKey)
                .blobName(fakeSource.name)
                .requestId(requestId)
                .retryPolicy(RetryPolicy.ofDefaults())
                .blobStoreApi(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var fakeSinkFactory = mock(BlobStoreApi.class);
        when(fakeSinkFactory.getBlobAdapter(
                eq(sinkAccountName),
                eq(sinkContainerName),
                eq(fakeSource.name),
                sharedAccessSignatureMatcher(sinkSharedAccessSignature)
        )).thenReturn(fakeSink);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedAccessSignature(sinkSharedAccessSignature)
                .requestId(requestId)
                .blobStoreApi(fakeSinkFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        var transferResult = dataSink.transfer(dataSource).get();
        assertThat(transferResult.failed()).isTrue();
        assertThat(transferResult.getFailureMessages()).containsExactly(format("Error reading blob %s", fakeSource.name));
    }

    /**
     * Verifies an exception thrown by the sink endpoint is handled correctly.
     */
    @Test
    void transfer_whenSinkFails_fails() throws Exception {

        // source completes normally
        var fakeSourceFactory = mock(BlobStoreApi.class);
        when(fakeSourceFactory.getBlobAdapter(
                sourceAccountName,
                sourceContainerName,
                fakeSource.name,
                sourceSharedKey
        )).thenReturn(fakeSource);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(sourceAccountName)
                .containerName(sourceContainerName)
                .sharedKey(sourceSharedKey)
                .blobName(fakeSource.name)
                .requestId(requestId)
                .retryPolicy(RetryPolicy.ofDefaults())
                .blobStoreApi(fakeSourceFactory)
                .monitor(monitor)
                .build();

        // sink endpoint raises an exception
        var blobAdapter = mock(BlobAdapter.class);
        when(blobAdapter.getOutputStream()).thenThrow(new RuntimeException());
        var fakeSinkFactory = mock(BlobStoreApi.class);
        when(fakeSinkFactory.getBlobAdapter(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(blobAdapter);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedAccessSignature(sinkSharedAccessSignature)
                .requestId(requestId)
                .blobStoreApi(fakeSinkFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().failed()).isTrue();
    }

    private static class FakeBlobAdapter implements BlobAdapter {
        private final String name = "test-name";
        private final String content = "test-content";
        private final long length = new Random().nextLong();
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(content.getBytes(UTF_8));
        }

        @Override
        public String getBlobName() {
            return name;
        }

        @Override
        public long getBlobSize() {
            return length;
        }
    }
}
