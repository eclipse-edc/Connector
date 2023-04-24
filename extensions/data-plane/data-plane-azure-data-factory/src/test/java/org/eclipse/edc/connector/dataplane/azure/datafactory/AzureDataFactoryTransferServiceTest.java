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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureDataFactoryTransferServiceTest {

    private final AzureDataFactoryTransferRequestValidator validator = mock(AzureDataFactoryTransferRequestValidator.class);
    private final AzureDataFactoryTransferManager transferManager = mock(AzureDataFactoryTransferManager.class);
    private final AzureDataFactoryTransferService transferService = new AzureDataFactoryTransferService(
            validator,
            transferManager);

    private final DataFlowRequest.Builder request = createRequest(AzureBlobStoreSchema.TYPE);
    private final Result<Boolean> failure = Result.failure("Test Failure");
    private final Result<Boolean> success = Result.success(true);
    @SuppressWarnings("unchecked")
    private final CompletableFuture<StreamResult<Void>> result = mock(CompletableFuture.class);

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void canHandle_onResult(boolean expected) {
        // Arrange
        when(validator.canHandle(request.build())).thenReturn(expected);
        // Act & Assert
        assertThat(transferService.canHandle(request.build())).isEqualTo(expected);
    }

    @Test
    void validate_onSuccess() {
        // Arrange
        when(validator.validate(request.build())).thenReturn(success);
        // Act & Assert
        assertThat(transferService.validate(request.build())).isSameAs(success);
    }

    @Test
    void validate_onFailure() {
        // Arrange
        when(validator.validate(request.build())).thenReturn(failure);
        // Act & Assert
        assertThat(transferService.validate(request.build())).isSameAs(failure);
    }

    @Test
    void transfer() {
        // Arrange
        when(transferManager.transfer(request.build())).thenReturn(result);
        // Act & Assert
        assertThat(transferService.transfer(request.build())).isSameAs(result);
    }
}
