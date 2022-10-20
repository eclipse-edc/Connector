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

package org.eclipse.edc.gcp.storage;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import org.eclipse.edc.gcp.common.GcpException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageServiceImplTest {
    private Storage storageMock;
    private StorageService storageService;


    @BeforeEach
    void setUp() {
        var monitor = Mockito.mock(Monitor.class);
        storageMock = Mockito.mock(Storage.class, RETURNS_SMART_NULLS);
        storageService = new StorageServiceImpl(storageMock, monitor);
    }

    @Test
    void getOrCreateBucketCreatesNewBucket() {
        var expectedBucket = mock(Bucket.class);
        var bucketName = "test-bucket";
        var bucketLocation = "test-location";
        when(expectedBucket.getName()).thenReturn(bucketName);
        when(expectedBucket.getLocation()).thenReturn(bucketLocation);

        var expectedBucketInfo = BucketInfo.newBuilder(bucketName).setLocation(bucketLocation).build();

        when(storageMock.get(bucketName)).thenReturn(null);

        when(storageMock.create(eq(expectedBucketInfo))).thenReturn(expectedBucket);

        assertThat(storageService.getOrCreateEmptyBucket(bucketName, bucketLocation).getName()).isEqualTo(bucketName);
    }

    @Test
    void getOrCreateBucketReturnsExistingBucket() {
        var existingBucket = mock(Bucket.class);
        var bucketName = "test-bucket";
        var bucketLocation = "test-location";

        when(existingBucket.getName()).thenReturn(bucketName);
        when(existingBucket.getLocation()).thenReturn(bucketLocation);

        when(storageMock.get(bucketName)).thenReturn(existingBucket);

        assertThat(storageService.getOrCreateEmptyBucket(bucketName, bucketLocation).getName()).isEqualTo(bucketName);
    }

    @Test
    void getOrCreateBucketFailsIfBucketExistsInWrongRegion() {
        var existingBucket = mock(Bucket.class);
        var bucketName = "test-bucket";
        var bucketLocation = "test-location";

        when(existingBucket.getName()).thenReturn(bucketName);
        when(existingBucket.getLocation()).thenReturn("other-location");

        when(storageMock.get(bucketName)).thenReturn(existingBucket);

        assertThatThrownBy(() -> storageService.getOrCreateEmptyBucket(bucketName, bucketLocation)).isInstanceOf(GcpException.class);
    }

    @Test
    void isEmptyNoBlobsInBucketReturnsTrue() {
        var blobList = new ArrayList<>();
        var blobPage = mock(Page.class);
        when(blobPage.getValues()).thenReturn(blobList);
        when(storageMock.list(eq("test-bucket"), any())).thenReturn(blobPage);

        assertThat(storageService.isEmpty("test-bucket")).isTrue();
    }

    @Test
    void isEmptyBlobFoundInBucketReturnsFalse() {
        var blobList = List.of(mock(Blob.class));
        var blobPage = mock(Page.class);
        when(blobPage.getValues()).thenReturn(blobList);
        when(storageMock.list(eq("test-bucket"), any())).thenReturn(blobPage);

        assertThat(storageService.isEmpty("test-bucket")).isFalse();
    }

}