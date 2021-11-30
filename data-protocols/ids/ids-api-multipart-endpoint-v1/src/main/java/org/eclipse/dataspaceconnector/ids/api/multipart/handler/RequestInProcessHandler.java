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

import de.fraunhofer.iais.eis.ContractOfferMessage;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.internalRecipientError;

/**
 * This class handles and processes incoming IDS {@link RequestInProcessHandler}s.
 */
public class RequestInProcessHandler implements Handler {

    private final Monitor monitor;
    private final String connectorId;

    public RequestInProcessHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof ContractOfferMessage;
    }

    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull VerificationResult verificationResult) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(verificationResult);

        var message = (RequestInProcessMessage) multipartRequest.getHeader();
        var correlationMessageId = message.getCorrelationMessage();
        monitor.debug(String.format("RequestInProcessHandler: Received response " +
                "to message %s. Payload: %s", correlationMessageId, multipartRequest.getPayload()));

        // TODO null will cause a RejectionMessage
        return MultipartResponse.Builder.newInstance()
                .header(internalRecipientError(message, connectorId))
                .build();
    }
}
