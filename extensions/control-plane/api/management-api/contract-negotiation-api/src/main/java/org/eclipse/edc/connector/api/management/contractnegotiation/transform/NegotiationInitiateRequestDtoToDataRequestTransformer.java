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

package org.eclipse.edc.connector.api.management.contractnegotiation.transform;

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestData;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;

public class NegotiationInitiateRequestDtoToDataRequestTransformer implements DtoTransformer<NegotiationInitiateRequestDto, ContractRequest> {

    private final Clock clock;

    /**
     * Instantiates the NegotiationInitiateRequestDtoToDataRequestTransformer.
     * <p>
     * If the {@link NegotiationInitiateRequestDto#getConsumerId()} is null, the default consumer ID is used.
     * IF the {@link NegotiationInitiateRequestDto#getProviderId()} is null, the connector address is used instead
     *
     * @param clock the time base for the contract offer transformation
     */
    public NegotiationInitiateRequestDtoToDataRequestTransformer(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Class<NegotiationInitiateRequestDto> getInputType() {
        return NegotiationInitiateRequestDto.class;
    }

    @Override
    public Class<ContractRequest> getOutputType() {
        return ContractRequest.class;
    }

    @Override
    public @Nullable ContractRequest transform(@NotNull NegotiationInitiateRequestDto object, @NotNull TransformerContext context) {
        // TODO: ContractOfferRequest should contain only the contractOfferId and the contract offer should be retrieved from the catalog. Ref #985
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(object.getOffer().getOfferId())
                .assetId(object.getOffer().getAssetId())
                .providerId(getId(object.getProviderId(), object.getConnectorAddress()))
                .policy(object.getOffer().getPolicy())
                .build();
        
        var requestData = ContractRequestData.Builder.newInstance()
                .connectorId(object.getConnectorId())
                .counterPartyAddress(object.getConnectorAddress())
                .protocol(object.getProtocol())
                .contractOffer(contractOffer)
                .build();

        return ContractRequest.Builder.newInstance()
                .requestData(requestData)
                .callbackAddresses(object.getCallbackAddresses())
                .build();
    }

    private String getId(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
