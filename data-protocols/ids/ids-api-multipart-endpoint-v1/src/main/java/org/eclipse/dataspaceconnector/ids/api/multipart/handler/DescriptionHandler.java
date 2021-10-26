/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.messageTypeNotSupported;

public class DescriptionHandler implements Handler {
    private final DescriptionHandlerSettings descriptionHandlerSettings;
    private final ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler;

    public DescriptionHandler(
            DescriptionHandlerSettings descriptionHandlerSettings,
            ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler) {
        this.descriptionHandlerSettings = descriptionHandlerSettings;
        this.connectorDescriptionRequestHandler = connectorDescriptionRequestHandler;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof DescriptionRequestMessage;
    }

    @Override
    public MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        var descriptionRequestMessage = (DescriptionRequestMessage) multipartRequest.getHeader();

        var requestedElement = descriptionRequestMessage.getRequestedElement();
        IdsId.Type type = null;
        if (requestedElement != null) {
            type = IdsId.fromUri(requestedElement).getType();
        }

        if (type == null || type == IdsId.Type.CONNECTOR) {
            return connectorDescriptionRequestHandler.handle(descriptionRequestMessage, multipartRequest.getPayload());
        }

        return createErrorMultipartResponse(descriptionRequestMessage);
    }

    private MultipartResponse createErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageTypeNotSupported(message, descriptionHandlerSettings.getId()))
                .build();
    }
}
