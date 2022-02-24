/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.NotificationMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.messageTypeNotSupported;

/**
 * Implementation of the {@link Handler} class for handling of {@link NotificationMessage}
 */
public class NotificationMessageHandler implements Handler {

    private final String connectorId;
    private final NotificationMessageHandlerRegistry subhandlers;

    public NotificationMessageHandler(String connectorId, @NotNull NotificationMessageHandlerRegistry subhandlers) {
        this.connectorId = connectorId;
        this.subhandlers = subhandlers;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        if (!(multipartRequest.getHeader() instanceof NotificationMessage)) {
            return false;
        }
        var notification = (NotificationMessage) multipartRequest.getHeader();
        return subhandlers.getHandler(notification.getClass()) != null;
    }

    /**
     * Delegate the processing of the request to the first {@link Handler} in the registry that accepts this kind of request, if any.
     */
    @Override
    public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull Result<ClaimToken> verificationResult) {
        var notification = (NotificationMessage) multipartRequest.getHeader();
        var subhandler = subhandlers.getHandler(notification.getClass());
        if (subhandler == null) {
            return createErrorMultipartResponse(multipartRequest.getHeader());
        }
        return subhandler.handleRequest(multipartRequest, verificationResult);
    }

    private MultipartResponse createErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageTypeNotSupported(message, connectorId))
                .build();
    }
}
