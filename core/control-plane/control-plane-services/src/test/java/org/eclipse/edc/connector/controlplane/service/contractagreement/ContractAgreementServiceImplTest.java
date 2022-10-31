/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.service.contractagreement;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractAgreementServiceImplTest {

    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractAgreementServiceImpl service = new ContractAgreementServiceImpl(store, transactionContext);

    @Test
    void findById_filtersById() {
        var agreement = createContractAgreement("agreementId");
        when(store.findContractAgreement("agreementId")).thenReturn(agreement);

        var result = service.findById("agreementId");

        assertThat(result).matches(it -> it.getId().equals("agreementId"));
    }

    @Test
    void findById_returnsNullIfNotFound() {
        when(store.findContractAgreement("agreementId")).thenReturn(null);

        var result = service.findById("agreementId");

        assertThat(result).isNull();
    }

    @Test
    void query_filtersBySpec() {
        var agreement = createContractAgreement("agreementId");
        when(store.queryAgreements(isA(QuerySpec.class))).thenReturn(Stream.of(agreement));

        var result = service.query(QuerySpec.none());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(1).first().matches(it -> it.getId().equals("agreementId"));
    }

    private ContractAgreement createContractAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerAgentId(UUID.randomUUID().toString())
                .consumerAgentId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}
