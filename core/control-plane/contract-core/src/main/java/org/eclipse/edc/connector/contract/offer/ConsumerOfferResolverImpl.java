/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.contract.spi.offer.ConsumerOfferResolver;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.validation.ValidatableConsumerOffer;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class ConsumerOfferResolverImpl implements ConsumerOfferResolver {

    private final ContractDefinitionStore contractDefinitionStore;
    private final PolicyDefinitionStore policyDefinitionStore;


    public ConsumerOfferResolverImpl(ContractDefinitionStore contractDefinitionStore, PolicyDefinitionStore policyDefinitionStore) {
        this.contractDefinitionStore = contractDefinitionStore;
        this.policyDefinitionStore = policyDefinitionStore;
    }

    @Override
    public @NotNull ServiceResult<ValidatableConsumerOffer> resolveOffer(String offerId) {
        var parsedResult = ContractOfferId.parseId(offerId);

        if (parsedResult.failed()) {
            return ServiceResult.badRequest(parsedResult.getFailureMessages());
        } else {
            var definitionId = parsedResult.getContent().definitionPart();
            var contractDefinition = contractDefinitionStore.findById(definitionId);
            if (contractDefinition == null) {
                return ServiceResult.notFound(format("Contract definition with id %s not found", definitionId));
            }

            var accessPolicy = policyDefinitionStore.findById(contractDefinition.getAccessPolicyId());
            if (accessPolicy == null) {
                return ServiceResult.notFound(format("Policy with id %s not found", contractDefinition.getAccessPolicyId()));
            }

            var contractPolicy = policyDefinitionStore.findById(contractDefinition.getContractPolicyId());
            if (contractPolicy == null) {
                return ServiceResult.notFound(format("Policy with id %s not found", contractDefinition.getContractPolicyId()));
            }

            return ServiceResult.success(ValidatableConsumerOffer.Builder.newInstance()
                    .contractDefinition(contractDefinition)
                    .accessPolicy(accessPolicy.getPolicy())
                    .contractPolicy(contractPolicy.getPolicy())
                    .offerId(parsedResult.getContent())
                    .build());

        }
    }

}
