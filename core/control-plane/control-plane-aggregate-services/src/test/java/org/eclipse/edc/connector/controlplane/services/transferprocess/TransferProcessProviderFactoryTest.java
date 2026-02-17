/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessProviderFactoryTest {

    private final AssetIndex assetIndex = mock();
    private final TransferProcessProviderFactory factory = new TransferProcessProviderFactory(mock(), mock(), assetIndex);

    @Test
    void shouldCreateProviderTransferProcess() {
        var dataplaneMetadata = DataplaneMetadata.Builder.newInstance().build();
        when(assetIndex.findById("assetId")).thenReturn(Asset.Builder.newInstance().id("assetId").dataplaneMetadata(dataplaneMetadata).build());
        var contractAgreement = createAgreementBuilder().assetId("assetId").build();

        var result = factory.create(createParticipantContext("participantContextId"), createMessage(), contractAgreement);

        assertThat(result).isSucceeded().satisfies(transferProcess -> {
            assertThat(transferProcess.getId()).isNotBlank();
            assertThat(transferProcess.getParticipantContextId()).isEqualTo("participantContextId");
            assertThat(transferProcess.getDataplaneMetadata()).isSameAs(dataplaneMetadata);
        });
        verify(assetIndex).findById("assetId");
    }

    @Test
    void shouldReturnError_whenAssetNotFound() {
        when(assetIndex.findById(any())).thenReturn(null);
        var contractAgreement = createAgreementBuilder().assetId("assetId").build();

        var result = factory.create(createParticipantContext("participantContextId"), createMessage(), contractAgreement);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
    }

    private ContractAgreement.Builder createAgreementBuilder() {
        return ContractAgreement.Builder.newInstance().providerId("providerId").consumerId("consumerId")
                .policy(Policy.Builder.newInstance().build());
    }

    private TransferRequestMessage createMessage() {
        return TransferRequestMessage.Builder.newInstance().callbackAddress("http://any").build();
    }

    private ParticipantContext createParticipantContext(String participantContextId) {
        return ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity("any").build();
    }
}
