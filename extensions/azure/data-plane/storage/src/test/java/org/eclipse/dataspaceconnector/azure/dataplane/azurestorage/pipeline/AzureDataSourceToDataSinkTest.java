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

import com.github.javafaker.Faker;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.FakeBlobAdapter;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureDataSourceToDataSinkTest {
    static Faker faker = new Faker();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    Monitor monitor = mock(Monitor.class);
    FakeBlobAdapter fakeSource = new FakeBlobAdapter();
    FakeBlobAdapter fakeSink = new FakeBlobAdapter();
    String sourceAccountName = AzureStorageTestFixtures.createAccountName();
    String sourceContainerName = AzureStorageTestFixtures.createContainerName();
    String sourceSharedKey = AzureStorageTestFixtures.createSharedKey();
    String sinkAccountName = AzureStorageTestFixtures.createAccountName();
    String sinkContainerName = AzureStorageTestFixtures.createContainerName();
    String sinkSharedKey = AzureStorageTestFixtures.createSharedKey();
    String requestId = UUID.randomUUID().toString();
    String errorMessage = faker.lorem().sentence();

    /**
     * Verifies a sink is able to pull data from the source without exceptions if both endpoints are functioning.
     */
    @Test
    void transfer_success() {
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
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
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var fakeSinkFactory = mock(BlobAdapterFactory.class);
        when(fakeSinkFactory.getBlobAdapter(
                sinkAccountName,
                sinkContainerName,
                fakeSource.name,
                sinkSharedKey
        )).thenReturn(fakeSink);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedKey(sinkSharedKey)
                .requestId(requestId)
                .blobAdapterFactory(fakeSinkFactory)
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
        when(blobAdapter.openInputStream()).thenThrow(new RuntimeException(errorMessage));
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
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
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var fakeSinkFactory = mock(BlobAdapterFactory.class);
        when(fakeSinkFactory.getBlobAdapter(
                sinkAccountName,
                sinkContainerName,
                fakeSource.name,
                sinkSharedKey
        )).thenReturn(fakeSink);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedKey(sinkSharedKey)
                .requestId(requestId)
                .blobAdapterFactory(fakeSinkFactory)
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
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
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
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        // sink endpoint raises an exception
        var blobAdapter = mock(BlobAdapter.class);
        when(blobAdapter.getOutputStream()).thenThrow(new RuntimeException());
        var fakeSinkFactory = mock(BlobAdapterFactory.class);
        when(fakeSinkFactory.getBlobAdapter(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(blobAdapter);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedKey(sinkSharedKey)
                .requestId(requestId)
                .blobAdapterFactory(fakeSinkFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().failed()).isTrue();
    }

}
