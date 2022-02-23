/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.ContractRejectionMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class handles and processes incoming IDS {@link ContractRejectionMessage}s.
 */
public class ContractRejectionHandler implements Handler {

    private final Monitor monitor;
    private final ProviderContractNegotiationManager providerNegotiationManager;
    private final ConsumerContractNegotiationManager consumerNegotiationManager;
    private final MessageFactory messageFactory;

    public ContractRejectionHandler(
            @NotNull Monitor monitor,
            @NotNull ProviderContractNegotiationManager providerNegotiationManager,
            @NotNull ConsumerContractNegotiationManager consumerNegotiationManager,
            @NotNull MessageFactory messageFactory) {
        this.monitor = Objects.requireNonNull(monitor);
        this.providerNegotiationManager = Objects.requireNonNull(providerNegotiationManager);
        this.consumerNegotiationManager = Objects.requireNonNull(consumerNegotiationManager);
        this.messageFactory = Objects.requireNonNull(messageFactory);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractRejectionMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull Result<ClaimToken> verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var message = (ContractRejectionMessage) multipartRequest.getHeader();
        var rejectionReason = message.getContractRejectionReason();
        var correlationMessageUri = message.getCorrelationMessage();
        var correlationMessageIdsId = IdsIdParser.parse(correlationMessageUri.toString());
        if (correlationMessageIdsId.getType() != IdsType.MESSAGE) {
            monitor.debug(String.format("ContractRejectionHandler: Expected message id of type %s but was %s.", IdsType.MESSAGE.getValue(), correlationMessageIdsId.getType().getValue()));
            return createBadParametersErrorMultipartResponse(message);
        }

        // abort negotiation process (one of them can handle this process by id)
        var token = verificationResult.getContent();
        var result = providerNegotiationManager.declined(token, correlationMessageIdsId.getValue());
        if (result.failed() && result.getStatus() == NegotiationResult.Status.FATAL_ERROR) {
            result = consumerNegotiationManager.declined(token, correlationMessageIdsId.getValue());
        }

        if (result.failed() && result.getStatus() == NegotiationResult.Status.FATAL_ERROR) {
            monitor.debug("ContractRejectionHandler: Could not process contract rejection");
            return createBadParametersErrorMultipartResponse(message);
        }

        monitor.debug(String.format("ContractRejectionHandler: Received contract rejection to " +
                "message %s. Rejection Reason: %s", correlationMessageIdsId.getValue(), rejectionReason));

        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.createMessageProcessedNotificationMessage(message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.badParameters(message))
                .build();
    }
}
