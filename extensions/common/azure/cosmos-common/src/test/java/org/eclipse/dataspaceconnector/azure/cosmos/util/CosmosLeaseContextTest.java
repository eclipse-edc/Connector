/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos.util;

import com.azure.cosmos.implementation.BadRequestException;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CosmosLeaseContextTest {

    public static final String TEST_PARTITION_KEY = "test-partition-key";
    private CosmosLeaseContext context;
    private CosmosDbApi cosmosDbApiMock;

    @BeforeEach
    void setUp() {
        cosmosDbApiMock = mock(CosmosDbApi.class);
        context = CosmosLeaseContext.with(cosmosDbApiMock, TEST_PARTITION_KEY, "me");
    }

    @Test
    void breakLease() {
        when(cosmosDbApiMock.invokeStoredProcedure(eq("lease"), eq(TEST_PARTITION_KEY), any())).thenReturn(null);
        context.breakLease("test-doc-id");
        verify(cosmosDbApiMock).invokeStoredProcedure("lease", TEST_PARTITION_KEY, "test-doc-id", "me", false);
    }

    @Test
    void breakLease_throwsException() {
        when(cosmosDbApiMock.invokeStoredProcedure(eq("lease"), eq(TEST_PARTITION_KEY), any())).thenThrow(new BadRequestException("foo"));
        assertThatThrownBy(() -> context.breakLease("test-doc-id")).isInstanceOf(BadRequestException.class);
        verify(cosmosDbApiMock, times(1)).invokeStoredProcedure("lease", TEST_PARTITION_KEY, "test-doc-id", "me", false);
    }

    @Test
    void acquireLease() {
        when(cosmosDbApiMock.invokeStoredProcedure(eq("lease"), eq(TEST_PARTITION_KEY), any())).thenReturn(null);
        context.acquireLease("test-doc-id");
        verify(cosmosDbApiMock).invokeStoredProcedure("lease", TEST_PARTITION_KEY, "test-doc-id", "me", true);
    }

    @Test
    void acquireLease_throwsException() {
        when(cosmosDbApiMock.invokeStoredProcedure(eq("lease"), eq(TEST_PARTITION_KEY), any())).thenThrow(new BadRequestException("foo"));
        assertThatThrownBy(() -> context.acquireLease("test-doc-id")).isInstanceOf(BadRequestException.class);
        verify(cosmosDbApiMock, times(1)).invokeStoredProcedure("lease", TEST_PARTITION_KEY, "test-doc-id", "me", true);
    }
}