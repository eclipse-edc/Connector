/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.gcp.lib.storage;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import org.eclipse.dataspaceconnector.gcp.lib.common.GcpExtensionException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageServiceImplTest {
    private Storage storageMock;
    private StorageService storageService;


    @BeforeEach
    void setUp() {
        Monitor monitor = Mockito.mock(Monitor.class);
        StorageClientFactory clientFactory = Mockito.mock(StorageClientFactory.class, RETURNS_SMART_NULLS);
        storageMock = Mockito.mock(Storage.class, RETURNS_SMART_NULLS);
        given(clientFactory.create()).willReturn(storageMock);
        storageService = new StorageServiceImpl(clientFactory, monitor);
    }

    @Test
    void getOrCreateBucketCreatesNewBucket() {
        Bucket expectedBucket = mock(Bucket.class);
        String bucketName = "test-bucket";
        String bucketLocation = "test-location";
        given(expectedBucket.getName()).willReturn(bucketName);
        given(expectedBucket.getLocation()).willReturn(bucketLocation);

        BucketInfo expectedBucketInfo = BucketInfo.newBuilder(bucketName).setLocation(bucketLocation).build();

        given(storageMock.get(bucketName)).willReturn(null);

        given(storageMock.create(eq(expectedBucketInfo))).willReturn(expectedBucket);

        assertThat(storageService.getOrCreateBucket(bucketName, bucketLocation).getName()).isEqualTo(bucketName);
    }

    @Test
    void getOrCreateBucketReturnsExistingBucket() {
        Bucket existingBucket = mock(Bucket.class);
        String bucketName = "test-bucket";
        String bucketLocation = "test-location";

        given(existingBucket.getName()).willReturn(bucketName);
        given(existingBucket.getLocation()).willReturn(bucketLocation);

        given(storageMock.get(bucketName)).willReturn(existingBucket);

        assertThat(storageService.getOrCreateBucket(bucketName, bucketLocation).getName()).isEqualTo(bucketName);
    }

    @Test
    void getOrCreateBucketFailsIfBucketExistsInWrongRegion() {
        Bucket existingBucket = mock(Bucket.class);
        String bucketName = "test-bucket";
        String bucketLocation = "test-location";

        given(existingBucket.getName()).willReturn(bucketName);
        given(existingBucket.getLocation()).willReturn("other-location");

        given(storageMock.get(bucketName)).willReturn(existingBucket);

        assertThatThrownBy(() -> storageService.getOrCreateBucket(bucketName, bucketLocation)).isInstanceOf(GcpExtensionException.class);
    }

    @Test
    void isEmptyNoBlobsInBucketReturnsTrue() {
        List<Blob> blobList = new ArrayList<>();
        Page<Blob> blobPage = mock(Page.class);
        when(blobPage.getValues()).thenReturn(blobList);
        when(storageMock.list(eq("test-bucket"), any())).thenReturn(blobPage);

        assertThat(storageService.isEmpty("test-bucket")).isTrue();
    }

    @Test
    void isEmptyBlobFoundInBucketReturnsFalse() {
        List<Blob> blobList = List.of(mock(Blob.class));
        Page<Blob> blobPage = mock(Page.class);
        when(blobPage.getValues()).thenReturn(blobList);
        when(storageMock.list(eq("test-bucket"), any())).thenReturn(blobPage);

        assertThat(storageService.isEmpty("test-bucket")).isFalse();
    }

}