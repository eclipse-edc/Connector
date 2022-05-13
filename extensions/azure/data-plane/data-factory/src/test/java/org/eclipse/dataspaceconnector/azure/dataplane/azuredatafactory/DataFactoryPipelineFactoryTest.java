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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DataFactoryPipelineFactoryTest {
    static final Faker FAKER = new Faker();

    DataFactoryClient client = mock(DataFactoryClient.class, RETURNS_DEEP_STUBS);
    TypeManager typeManager = new TypeManager();
    KeyVaultClient keyVaultClient = mock(KeyVaultClient.class);
    AzureSasToken azureSasToken = new AzureSasToken(FAKER.lorem().word(), FAKER.number().randomNumber());
    KeyVaultSecret writeOnlySasSecret = new KeyVaultSecret(FAKER.lorem().word(), typeManager.writeValueAsString(azureSasToken));
    KeyVaultSecret destinationSecret = new KeyVaultSecret(FAKER.lorem().word(), FAKER.lorem().word());

    DataFlowRequest request = AzureDataFactoryTransferRequestValidatorTest.requestWithProperties;

    String keyVaultLinkedService = FAKER.lorem().word();
    DataFactoryPipelineFactory factory = new DataFactoryPipelineFactory(keyVaultLinkedService, keyVaultClient, client, typeManager);

    @Test
    void createPipeline() {
        when(keyVaultClient.getSecret(request.getDestinationDataAddress().getKeyName()))
                .thenReturn(writeOnlySasSecret);
        when(keyVaultClient.setSecret(any(), eq(azureSasToken.getSas())))
                .thenReturn(destinationSecret);

        factory.createPipeline(request);

        verify(client).definePipeline(any());
        verify(client, times(2)).defineDataset(any());
        verify(client, times(2)).defineLinkedService(any());
        verifyNoMoreInteractions(client);

        verify(keyVaultClient, times(1)).getSecret(any());
        verify(keyVaultClient, times(1)).setSecret(any(), any());
        verifyNoMoreInteractions(keyVaultClient);
    }
}