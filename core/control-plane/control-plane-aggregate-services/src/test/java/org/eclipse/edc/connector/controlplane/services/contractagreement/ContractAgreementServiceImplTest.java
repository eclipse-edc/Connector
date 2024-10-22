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

package org.eclipse.edc.connector.controlplane.services.contractagreement;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractAgreementServiceImplTest {

    private final ContractNegotiationStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final QueryValidator queryValidator = mock();
    private final ContractAgreementService service = new ContractAgreementServiceImpl(store, transactionContext, queryValidator);

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
    void search_filtersBySpec() {
        var agreement = createContractAgreement("agreementId");
        when(store.queryAgreements(isA(QuerySpec.class))).thenReturn(Stream.of(agreement));
        when(queryValidator.validate(any())).thenReturn(Result.success());

        var result = service.search(QuerySpec.none());

        assertThat(result).isSucceeded().asInstanceOf(list(ContractAgreement.class))
                .hasSize(1).first().matches(it -> it.getId().equals("agreementId"));
    }

    @Test
    void search_shouldFail_whenQueryIsNotValid() {
        when(queryValidator.validate(any())).thenReturn(Result.failure("not valid"));

        var result = service.search(QuerySpec.none());

        assertThat(result).isFailed();
        verifyNoInteractions(store);
    }

    @Test
    void findNegotiation_shouldReturnNegotiationFilteredByAgreementId() {
        var negotiation = createContractNegotiation("negotiationId");
        when(store.queryNegotiations(any())).thenReturn(Stream.of(negotiation));

        var result = service.findNegotiation("agreementId");

        assertThat(result).isEqualTo(negotiation);
        var expectedCriterion = criterion("contractAgreement.id", "=", "agreementId");
        verify(store).queryNegotiations(argThat(q -> q.getFilterExpression().contains(expectedCriterion)));
    }

    @Test
    void findNegotiation_shouldReturnNull_whenAgreementOrNegotiationDoesNotExist() {
        when(store.queryNegotiations(any())).thenReturn(Stream.empty());

        var result = service.findNegotiation("agreementId");

        assertThat(result).isNull();
    }

    private ContractAgreement createContractAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private ContractNegotiation createContractNegotiation(String negotiationId) {
        return ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId(randomUUID().toString())
                .counterPartyAddress("address")
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .build()))
                .protocol("protocol")
                .build();
    }
}
