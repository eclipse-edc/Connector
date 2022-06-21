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
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionResponseMessageUtil.createDescriptionResponseMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.MultipartResponseUtil.createBadParametersErrorMultipartResponse;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.MultipartResponseUtil.createErrorMultipartResponse;

public class ConnectorDescriptionRequestHandler implements DescriptionRequestHandler {
    private final String connectorId;
    private final Monitor monitor;
    private final ConnectorService connectorService;
    private final IdsTransformerRegistry transformerRegistry;

    public ConnectorDescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull ConnectorService connectorService,
            @NotNull IdsTransformerRegistry transformerRegistry) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorService = Objects.requireNonNull(connectorService);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.connectorId = Objects.requireNonNull(connectorId);
    }

    @Override
    public MultipartResponse handle(@NotNull DescriptionRequestMessage descriptionRequestMessage,
                                    @NotNull ClaimToken claimToken,
                                    @Nullable String payload) {
        Objects.requireNonNull(claimToken);
        Objects.requireNonNull(descriptionRequestMessage);

        if (!isRequestingCurrentConnectorsDescription(descriptionRequestMessage)) {
            return createErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        var descriptionResponseMessage = createDescriptionResponseMessage(connectorId, descriptionRequestMessage);

        var transformResult = transformerRegistry.transform(connectorService.getConnector(claimToken), Connector.class);
        if (transformResult.failed()) {
            monitor.warning(
                    String.format(
                            "Could not transform Connector: [%s]",
                            String.join(", ", transformResult.getFailureMessages())
                    )
            );
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
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

        URI connectorIdUri = URI.create(String.join(
                IdsIdParser.DELIMITER,
                IdsIdParser.SCHEME,
                IdsType.CONNECTOR.getValue(),
                connectorId));

        return requestedConnectorId.equals(connectorIdUri);
    }
}
