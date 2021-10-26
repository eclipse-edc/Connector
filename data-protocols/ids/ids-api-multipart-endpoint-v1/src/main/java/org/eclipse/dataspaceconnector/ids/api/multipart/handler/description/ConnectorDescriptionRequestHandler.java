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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.factory.DescriptionResponseMessageFactory;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.service.ConnectorDescriptionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.messageTypeNotSupported;

public class ConnectorDescriptionRequestHandler implements DescriptionRequestHandler {
    private final DescriptionResponseMessageFactory descriptionResponseMessageFactory;
    private final ConnectorDescriptionService connectorDescriptionService;
    private final ConnectorDescriptionRequestHandlerSettings connectorDescriptionRequestHandlerSettings;

    public ConnectorDescriptionRequestHandler(
            @NotNull DescriptionResponseMessageFactory descriptionResponseMessageFactory,
            @NotNull ConnectorDescriptionService connectorDescriptionService,
            @NotNull ConnectorDescriptionRequestHandlerSettings connectorDescriptionRequestHandlerSettings) {
        this.descriptionResponseMessageFactory = Objects.requireNonNull(descriptionResponseMessageFactory);
        this.connectorDescriptionService = Objects.requireNonNull(connectorDescriptionService);
        this.connectorDescriptionRequestHandlerSettings = Objects.requireNonNull(connectorDescriptionRequestHandlerSettings);
    }

    @Override
    public MultipartResponse handle(@NotNull DescriptionRequestMessage descriptionRequestMessage, @Nullable String payload) {
        Objects.requireNonNull(descriptionRequestMessage);

        if (!isRequestingCurrentConnectorsDescription(descriptionRequestMessage)) {
            return createErrorMultipartResponse(descriptionRequestMessage);
        }

        DescriptionResponseMessage descriptionResponseMessage = descriptionResponseMessageFactory
                .createDescriptionResponseMessage(descriptionRequestMessage);

        Connector connector = connectorDescriptionService.createSelfDescription();

        return MultipartResponse.Builder.newInstance()
                .header(descriptionResponseMessage)
                .payload(connector)
                .build();
    }

    private boolean isRequestingCurrentConnectorsDescription(DescriptionRequestMessage descriptionRequestMessage) {
        URI requestedConnectorId = descriptionRequestMessage.getRequestedElement();
        URI connectorId = connectorDescriptionRequestHandlerSettings.getId();

        if (requestedConnectorId == null) {
            return true;
        }

        return requestedConnectorId.equals(connectorId);
    }

    private MultipartResponse createErrorMultipartResponse(Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageTypeNotSupported(message, connectorDescriptionRequestHandlerSettings.getId()))
                .build();
    }
}
