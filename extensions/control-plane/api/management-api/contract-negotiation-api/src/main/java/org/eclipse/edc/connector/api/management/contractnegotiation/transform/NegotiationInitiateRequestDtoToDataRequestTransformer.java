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
import java.util.Optional;

public class NegotiationInitiateRequestDtoToDataRequestTransformer implements DtoTransformer<NegotiationInitiateRequestDto, ContractOfferRequest> {

    private final Clock clock;

    public NegotiationInitiateRequestDtoToDataRequestTransformer(Clock clock) {
        this.clock = clock;
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
    public @Nullable ContractOfferRequest transform(@Nullable NegotiationInitiateRequestDto object, @NotNull TransformerContext context) {
        return Optional.ofNullable(object)
                .map(input -> {
                    // TODO: ContractOfferRequest should contain only the contractOfferId and the contract offer should be retrieved from the catalog. Ref #985
                    var contractOffer = ContractOffer.Builder.newInstance()
                            .id(input.getOffer().getOfferId())
                            .asset(Asset.Builder.newInstance().id(input.getOffer().getAssetId()).build())
                            // TODO: this is a workaround for the bug described in https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/753
                            .consumer(URI.create("urn:connector:consumer"))
                            .provider(URI.create("urn:connector:provider"))
                            .policy(input.getOffer().getPolicy())
                            .contractEnd(ZonedDateTime.ofInstant(clock.instant(), clock.getZone()).plusSeconds(input.getOffer().getValidity().toSeconds()))
                            .build();
                    return ContractOfferRequest.Builder.newInstance()
                            .connectorId(input.getConnectorId())
                            .connectorAddress(input.getConnectorAddress())
                            .protocol(input.getProtocol())
                            .contractOffer(contractOffer)
                            .type(ContractOfferRequest.Type.INITIAL)
                            .build();
                })
                .orElseGet(() -> {
                    context.reportProblem("input negotiation initiate request is null");
                    return null;
                });
    }
}
