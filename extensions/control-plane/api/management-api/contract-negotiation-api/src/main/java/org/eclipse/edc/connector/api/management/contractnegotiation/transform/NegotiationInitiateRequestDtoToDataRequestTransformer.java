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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Clock;
import java.time.ZonedDateTime;

public class NegotiationInitiateRequestDtoToDataRequestTransformer implements DtoTransformer<NegotiationInitiateRequestDto, ContractOfferRequest> {

    private final Clock clock;
    private final String defaultConsumerId;

    /**
     * Instantiates the NegotiationInitiateRequestDtoToDataRequestTransformer.
     * <p>
     * If the {@link NegotiationInitiateRequestDto#getConsumerId()} is null, the default consumer ID is used.
     * IF the {@link NegotiationInitiateRequestDto#getProviderId()} is null, the connector address is used instead
     *
     * @param clock             the time base for the contract offer transformation
     * @param defaultConsumerId The ID of the sending connector, in case the {@link NegotiationInitiateRequestDto#getConsumerId()} field is null
     */
    public NegotiationInitiateRequestDtoToDataRequestTransformer(Clock clock, String defaultConsumerId) {
        this.clock = clock;
        this.defaultConsumerId = defaultConsumerId;
    }

    @Override
    public Class<NegotiationInitiateRequestDto> getInputType() {
        return NegotiationInitiateRequestDto.class;
    }

    @Override
    public Class<ContractOfferRequest> getOutputType() {
        return ContractOfferRequest.class;
    }

    @Override
    public @Nullable ContractOfferRequest transform(@NotNull NegotiationInitiateRequestDto object, @NotNull TransformerContext context) {
        // TODO: ContractOfferRequest should contain only the contractOfferId and the contract offer should be retrieved from the catalog. Ref #985
        var now = ZonedDateTime.ofInstant(clock.instant(), clock.getZone());
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(object.getOffer().getOfferId())
                .asset(Asset.Builder.newInstance().id(object.getOffer().getAssetId()).build())
                .consumer(createUri(object.getConsumerId(), defaultConsumerId))
                .provider(createUri(object.getProviderId(), object.getConnectorAddress()))
                .policy(object.getOffer().getPolicy())
                .contractStart(now)
                .contractEnd(now.plusSeconds(object.getOffer().getValidity()))
                .build();
        return ContractOfferRequest.Builder.newInstance()
                .connectorId(object.getConnectorId())
                .connectorAddress(object.getConnectorAddress())
                .protocol(object.getProtocol())
                .contractOffer(contractOffer)
                .type(ContractOfferRequest.Type.INITIAL)
                .build();
    }

    private URI createUri(String value, String defaultValue) {
        return URI.create(value != null ? value : defaultValue);
    }
}
