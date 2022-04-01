/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering, Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Daimler TSS GmbH - introduce factory to create RequestInProcessMessage
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.ContractOfferMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.IdsResponseMessageFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions.InvalidCorrelationMessageException;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;

/**
 * This class handles and processes incoming IDS {@link ContractOfferMessage}s.
 */
public class ContractOfferHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final String connectorId;
    private final ProviderContractNegotiationManager providerNegotiationManager;
    private final ConsumerContractNegotiationManager consumerNegotiationManager;
    private final IdsResponseMessageFactory responseMessageFactory;

    public ContractOfferHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ProviderContractNegotiationManager providerNegotiationManager,
            @NotNull ConsumerContractNegotiationManager consumerNegotiationManager,
            @NotNull IdsResponseMessageFactory responseMessageFactory) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.providerNegotiationManager = Objects.requireNonNull(providerNegotiationManager);
        this.consumerNegotiationManager = Objects.requireNonNull(consumerNegotiationManager);
        this.responseMessageFactory = Objects.requireNonNull(responseMessageFactory);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractOfferMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull ClaimToken claimToken) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(claimToken);

        var message = (ContractOfferMessage) multipartRequest.getHeader();

        ContractOffer contractOffer = null;
        try {
            contractOffer = objectMapper.readValue(multipartRequest.getPayload(), ContractOffer.class);
        } catch (IOException e) {
            monitor.severe("ContractOfferHandler: Contract Offer is invalid", e);
            return createBadParametersErrorMultipartResponse(message);
        }

        Message response;

        try {
            response = responseMessageFactory.createRequestInProcessMessage(message);
        } catch (Exception e) {
            if (e instanceof InvalidCorrelationMessageException) {
                monitor.debug(String.format("Rejecting invalid IDS contract offer message [Msg-ID: %s]", message.getId()), e);
            } else {
                monitor.severe(String.format("Exception while creating IDS RequestInProcessMessage to answer contract offer [Msg-ID: %s]", message.getId()), e);
            }

            response = responseMessageFactory.createRejectionMessage(message, e);
        }

        if (response instanceof RequestInProcessMessage) {
            // TODO similar implementation to ContractRequestHandler (only required if counter offers supported, not needed for M1)
        }

        return MultipartResponse.Builder.newInstance()
                .header(response)
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }
}
