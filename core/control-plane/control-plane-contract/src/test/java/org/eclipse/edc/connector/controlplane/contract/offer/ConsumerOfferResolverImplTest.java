/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.contract.offer;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class ConsumerOfferResolverImplTest {

    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final ContractDefinitionStore definitionStore = mock(ContractDefinitionStore.class);
    private ConsumerOfferResolverImpl validatableConsumerOfferResolver;

    @BeforeEach
    void setUp() {
        validatableConsumerOfferResolver = new ConsumerOfferResolverImpl(definitionStore, policyStore);
    }

    @Test
    void resolveOffer() {
        var contractDefinition = createContractDefinition();

        var offerId = ContractOfferId.create(contractDefinition.getId(), "1");
        var accessPolicy = Policy.Builder.newInstance().build();
        var accessPolicyDef = PolicyDefinition.Builder.newInstance().policy(accessPolicy).build();
        var contractPolicy = Policy.Builder.newInstance().build();
        var contractPolicyDef = PolicyDefinition.Builder.newInstance().policy(contractPolicy).build();

        when(policyStore.findById(contractDefinition.getAccessPolicyId())).thenReturn(accessPolicyDef);
        when(policyStore.findById(contractDefinition.getContractPolicyId())).thenReturn(contractPolicyDef);
        when(definitionStore.findById(contractDefinition.getId())).thenReturn(contractDefinition);

        var validatableOfferResult = validatableConsumerOfferResolver.resolveOffer(offerId.toString());

        assertThat(validatableOfferResult).isSucceeded().satisfies(consumerOffer -> {
            assertThat(consumerOffer.getOfferId()).usingRecursiveComparison().isEqualTo(offerId);
            assertThat(consumerOffer.getContractDefinition()).isEqualTo(contractDefinition);
            assertThat(consumerOffer.getAccessPolicy()).isSameAs(accessPolicy);
            assertThat(consumerOffer.getContractPolicy()).isSameAs(contractPolicy);
        });

        verify(policyStore).findById(contractDefinition.getAccessPolicyId());
        verify(policyStore).findById(contractDefinition.getContractPolicyId());
        verify(definitionStore).findById(any());
    }

    @Test
    void resolveOffer_shouldReturnNotFound_whenContractDefinitionNotFound() {
        var offerId = ContractOfferId.create("1", "1");

        when(definitionStore.findById(any())).thenReturn(null);

        var validatableOfferResult = validatableConsumerOfferResolver.resolveOffer(offerId.toString());

        assertThat(validatableOfferResult).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);

        verify(definitionStore).findById(any());
    }

    @Test
    void resolveOffer_shouldReturnNotFound_whenAccessPolicyNotFound() {
        var contractDefinition = createContractDefinition();
        var offerId = ContractOfferId.create(contractDefinition.getId(), "1");
        var accessPolicy = Policy.Builder.newInstance().build();
        var accessPolicyDef = PolicyDefinition.Builder.newInstance().policy(accessPolicy).build();

        when(definitionStore.findById(contractDefinition.getId())).thenReturn(contractDefinition);
        when(policyStore.findById(contractDefinition.getAccessPolicyId())).thenReturn(accessPolicyDef);
        when(policyStore.findById(contractDefinition.getContractPolicyId())).thenReturn(null);

        var validatableOfferResult = validatableConsumerOfferResolver.resolveOffer(offerId.toString());

        assertThat(validatableOfferResult).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);

        verify(definitionStore).findById(any());
    }

    @Test
    void resolveOffer_shouldReturnNotFound_whenContractPolicyNotFound() {
        var contractDefinition = createContractDefinition();
        var offerId = ContractOfferId.create(contractDefinition.getId(), "1");

        when(definitionStore.findById(contractDefinition.getId())).thenReturn(contractDefinition);
        when(policyStore.findById(contractDefinition.getAccessPolicyId())).thenReturn(null);

        var validatableOfferResult = validatableConsumerOfferResolver.resolveOffer(offerId.toString());

        assertThat(validatableOfferResult).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);

        verify(definitionStore).findById(any());
    }

    @Test
    void resolveOffer_shouldReturnBadRequest_whenOfferIdParseFails() {

        var validatableOfferResult = validatableConsumerOfferResolver.resolveOffer("malformed");

        assertThat(validatableOfferResult).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);

    }

    private ContractDefinition createContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .build();
    }
}
