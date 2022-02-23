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

import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.ContractOfferMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * This class handles and processes incoming IDS {@link ContractOfferMessage}s.
 */
public class ContractOfferHandler implements Handler {

    private final Monitor monitor;
    private final Serializer serializer;
    private final MessageFactory messageFactory;

    public ContractOfferHandler(
            @NotNull Monitor monitor,
            @NotNull Serializer serializer,
            @NotNull MessageFactory messageFactory) {
        this.monitor = Objects.requireNonNull(monitor);
        this.serializer = Objects.requireNonNull(serializer);
        this.messageFactory = Objects.requireNonNull(messageFactory);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractOfferMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull Result<ClaimToken> verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var message = (ContractOfferMessage) multipartRequest.getHeader();

        ContractOffer contractOffer = null;
        try {
            contractOffer = serializer.deserialize(multipartRequest.getPayload(), ContractOffer.class);
        } catch (IOException e) {
            monitor.severe("ContractOfferHandler: Contract Offer is invalid", e);
            return createBadParametersErrorMultipartResponse(message);
        }

        // TODO similar implementation to ContractRequestHandler (only required if counter offers supported, not needed for M1)

        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.createRequestInProcessMessage(message))
                .build();
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageFactory.badParameters(message))
                .build();
    }
}
