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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.notifications;

import de.fraunhofer.iais.eis.ContractOfferMessage;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class handles and processes incoming IDS {@link MessageProcessedNotificationMessage}s.
 */
public class MessageProcessedNotificationHandler implements Handler {

    private final Monitor monitor;

    public MessageProcessedNotificationHandler(@NotNull Monitor monitor) {
        this.monitor = Objects.requireNonNull(monitor);
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

        var message = (MessageProcessedNotificationMessage) multipartRequest.getHeader();
        var correlationMessageId = message.getCorrelationMessage();
        monitor.debug(String.format("MessageProcessedNotificationHandler: Received response " +
                "to message %s. Payload: %s", correlationMessageId, multipartRequest.getPayload()));

        return null; // TODO this will cause a RejectionMessage
    }
}
