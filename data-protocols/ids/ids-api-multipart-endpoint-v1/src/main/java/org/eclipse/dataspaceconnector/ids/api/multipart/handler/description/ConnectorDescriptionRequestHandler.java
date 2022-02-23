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
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class ConnectorDescriptionRequestHandler implements DescriptionRequestHandler {
    private final URI connectorId;
    private final Monitor monitor;
    private final ConnectorService connectorService;
    private final TransformerRegistry transformerRegistry;
    private final MultipartResponseFactory multipartResponseFactory;
    private final MessageFactory messageFactory;

    public ConnectorDescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull URI connectorId,
            @NotNull ConnectorService connectorService,
            @NotNull TransformerRegistry transformerRegistry,
            @NotNull MultipartResponseFactory multipartResponseFactory,
            @NotNull MessageFactory messageFactory) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorService = Objects.requireNonNull(connectorService);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.multipartResponseFactory = Objects.requireNonNull(multipartResponseFactory);
        this.messageFactory = Objects.requireNonNull(messageFactory);
    }

    @Override
    public MultipartResponse handle(@NotNull DescriptionRequestMessage descriptionRequestMessage,
                                    @NotNull Result<ClaimToken> verificationResult,
                                    @Nullable String payload) {
        Objects.requireNonNull(verificationResult);
        Objects.requireNonNull(descriptionRequestMessage);

        if (!isRequestingCurrentConnectorsDescription(descriptionRequestMessage)) {
            return multipartResponseFactory.createErrorMultipartResponse(descriptionRequestMessage);
        }

        DescriptionResponseMessage descriptionResponseMessage = messageFactory.createDescriptionResponseMessage(descriptionRequestMessage);

        Result<Connector> transformResult = transformerRegistry.transform(connectorService.getConnector(verificationResult), Connector.class);
        if (transformResult.failed()) {
            monitor.warning(
                    String.format(
                            "Could not transform Connector: [%s]",
                            String.join(", ", transformResult.getFailureMessages())
                    )
            );
            return multipartResponseFactory.createBadParametersErrorMultipartResponse(descriptionRequestMessage);
        }

        Connector connector = transformResult.getContent();

        return MultipartResponse.Builder.newInstance()
                .header(descriptionResponseMessage)
                .payload(connector)
                .build();
    }

    private boolean isRequestingCurrentConnectorsDescription(DescriptionRequestMessage descriptionRequestMessage) {
        URI requestedConnectorId = descriptionRequestMessage.getRequestedElement();

        if (requestedConnectorId == null) {
            return true;
        }

        return requestedConnectorId.equals(connectorId);
    }
}
