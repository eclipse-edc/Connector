/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreementRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractAgreementRequestToIdsContractAgreementTransformerTest {

    private static final URI AGREEMENT_ID = URI.create("urn:agreement:456uz984390236s");
    private static final String PROVIDER_ID = "https://provider.com/";
    private final TransformerContext context = mock(TransformerContext.class);
    private final ContractAgreementRequestToIdsContractAgreementTransformer transformer = new ContractAgreementRequestToIdsContractAgreementTransformer();

    @Test
    void verifyInputType() {
        assertThat(transformer.getInputType()).isNotNull();
    }

    @Test
    void verifyOutputType() {
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void verifyTransform() {
        Permission edcPermission = mock(Permission.class);
        de.fraunhofer.iais.eis.Permission idsPermission = mock(de.fraunhofer.iais.eis.Permission.class);
        Prohibition edcProhibition = mock(Prohibition.class);
        de.fraunhofer.iais.eis.Prohibition idsProhibition = mock(de.fraunhofer.iais.eis.Prohibition.class);
        Duty edcObligation = mock(Duty.class);
        de.fraunhofer.iais.eis.Duty idsObligation = mock(de.fraunhofer.iais.eis.Duty.class);
        var policy = Policy.Builder.newInstance()
                .permission(edcPermission)
                .prohibition(edcProhibition)
                .duty(edcObligation)
                .build();

        when(context.transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class))).thenReturn(idsPermission);
        when(context.transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class))).thenReturn(idsProhibition);
        when(context.transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class))).thenReturn(idsObligation);
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(AGREEMENT_ID);

        var result = transformer.transform(contractAgreementRequest(policy), context);

        assertThat(result).isNotNull()
                .satisfies(it -> {
                    assertThat(it.getId()).isEqualTo(AGREEMENT_ID);
                    assertThat(result.getProvider()).isEqualTo(URI.create(PROVIDER_ID));
                    assertThat(result.getObligation()).hasSize(1).first().isEqualTo(idsObligation);
                    assertThat(result.getPermission()).hasSize(1).first().isEqualTo(idsPermission);
                    assertThat(result.getProhibition()).hasSize(1).first().isEqualTo(idsProhibition);
                });
        verify(context).transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class));
        verify(context).transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class));
        verify(context).transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class));
        verify(context).transform(isA(IdsId.class), eq(URI.class));
    }

    private ContractAgreementRequest contractAgreementRequest(Policy policy) {
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .id(String.valueOf(AGREEMENT_ID))
                .providerAgentId(PROVIDER_ID)
                .assetId(UUID.randomUUID().toString())
                .consumerAgentId("id")
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .contractSigningDate(Instant.now().getEpochSecond())
                .policy(policy)
                .build();

        return ContractAgreementRequest.Builder.newInstance()
                .contractAgreement(contractAgreement)
                .policy(policy)
                .protocol("any")
                .connectorId("any")
                .connectorAddress("any")
                .correlationId("any")
                .build();
    }
}