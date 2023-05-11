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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.transform.type.contract.ContractAgreementToIdsContractAgreementTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractAgreementToIdsContractAgreementTransformerTest {
    private static final String AGREEMENT_ID = "456uz984390236s";
    private static final URI AGREEMENT_ID_URI = URI.create("urn:contractagreement:" + AGREEMENT_ID);
    private static final String PROVIDER_ID = "https://provider.com/";
    private final TransformerContext context = mock(TransformerContext.class);
    private final ContractAgreementToIdsContractAgreementTransformer transformer = new ContractAgreementToIdsContractAgreementTransformer();

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

        var result = transformer.transform(contractAgreementRequest(policy), context);

        assertThat(result).isNotNull()
                .satisfies(it -> {
                    assertThat(it.getId()).isEqualTo(AGREEMENT_ID_URI);
                    assertThat(result.getProvider()).isEqualTo(URI.create(PROVIDER_ID));
                    assertThat(result.getObligation()).hasSize(1).first().isEqualTo(idsObligation);
                    assertThat(result.getPermission()).hasSize(1).first().isEqualTo(idsPermission);
                    assertThat(result.getProhibition()).hasSize(1).first().isEqualTo(idsProhibition);
                });
        verify(context).transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class));
        verify(context).transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class));
        verify(context).transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class));
    }

    private ContractAgreementMessage contractAgreementRequest(Policy policy) {
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .id(String.valueOf(AGREEMENT_ID))
                .providerId(PROVIDER_ID)
                .assetId(UUID.randomUUID().toString())
                .consumerId("id")
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .contractSigningDate(Instant.now().getEpochSecond())
                .policy(policy)
                .build();

        return ContractAgreementMessage.Builder.newInstance()
                .contractAgreement(contractAgreement)
                .policy(policy)
                .protocol("any")
                .connectorId("any")
                .counterPartyAddress("any")
                .processId("any")
                .build();
    }
}
