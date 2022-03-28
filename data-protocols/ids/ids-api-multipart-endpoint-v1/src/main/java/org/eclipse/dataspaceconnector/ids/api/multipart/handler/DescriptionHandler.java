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
 *       Daimler TSS GmbH - introduce factory to create IDS ResponseMessages
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.IdsResponseMessageFactory;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DescriptionHandler implements Handler {
    private final Monitor monitor;
    private final IdsTransformerRegistry transformerRegistry;
    private final ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler;
    private final DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler;
    private final RepresentationDescriptionRequestHandler representationDescriptionRequestHandler;
    private final ResourceDescriptionRequestHandler resourceDescriptionRequestHandler;
    private final ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler;
    private final IdsResponseMessageFactory responseMessageFactory;

    public DescriptionHandler(
            @NotNull Monitor monitor,
            @NotNull IdsTransformerRegistry transformerRegistry,
            @NotNull ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler,
            @NotNull DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler,
            @NotNull RepresentationDescriptionRequestHandler representationDescriptionRequestHandler,
            @NotNull ResourceDescriptionRequestHandler resourceDescriptionRequestHandler,
            @NotNull ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler,
            @NotNull IdsResponseMessageFactory responseMessageFactory) {
        this.monitor = Objects.requireNonNull(monitor);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.artifactDescriptionRequestHandler = Objects.requireNonNull(artifactDescriptionRequestHandler);
        this.dataCatalogDescriptionRequestHandler = Objects.requireNonNull(dataCatalogDescriptionRequestHandler);
        this.representationDescriptionRequestHandler = Objects.requireNonNull(representationDescriptionRequestHandler);
        this.resourceDescriptionRequestHandler = Objects.requireNonNull(resourceDescriptionRequestHandler);
        this.connectorDescriptionRequestHandler = Objects.requireNonNull(connectorDescriptionRequestHandler);
        this.responseMessageFactory = Objects.requireNonNull(responseMessageFactory);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof DescriptionRequestMessage;
    }

    @Override
    public MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest,
                                           @NotNull ClaimToken claimToken) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(claimToken);

        try {
            return handleRequestInternal(multipartRequest, claimToken);
        } catch (EdcException exception) {
            monitor.severe(String.format("Could not handle multipart request: %s", exception.getMessage()), exception);
        }

        return createErrorMultipartResponse(multipartRequest.getHeader());
    }

    public MultipartResponse handleRequestInternal(@NotNull MultipartRequest multipartRequest,
                                                   @NotNull ClaimToken claimToken) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(claimToken);

        var descriptionRequestMessage = (DescriptionRequestMessage) multipartRequest.getHeader();

        var payload = multipartRequest.getPayload();

        var requestedElement = descriptionRequestMessage.getRequestedElement();
        IdsId idsId = null;
        if (requestedElement != null) {
            var result = transformerRegistry.transform(requestedElement, IdsId.class);
            if (result.failed()) {
                monitor.warning(
                        String.format(
                                "Could not transform URI to IdsId: [%s]",
                                String.join(", ", result.getFailureMessages())
                        )
                );
                return createBadParametersErrorMultipartResponse(descriptionRequestMessage);
            }

            idsId = result.getContent();
        }

        IdsType type;
        if (idsId == null || (type = idsId.getType()) == IdsType.CONNECTOR) {
            return connectorDescriptionRequestHandler.handle(descriptionRequestMessage, claimToken, payload);
        }

        switch (type) {
            case ARTIFACT:
                return artifactDescriptionRequestHandler.handle(descriptionRequestMessage, claimToken, payload);
            case CATALOG:
                return dataCatalogDescriptionRequestHandler.handle(descriptionRequestMessage, claimToken, payload);
            case REPRESENTATION:
                return representationDescriptionRequestHandler.handle(descriptionRequestMessage, claimToken, payload);
            case RESOURCE:
                return resourceDescriptionRequestHandler.handle(descriptionRequestMessage, claimToken, payload);
            default:
                return createErrorMultipartResponse(descriptionRequestMessage);
        }
    }

    private MultipartResponse createBadParametersErrorMultipartResponse(Message message) {
        RejectionMessage badParametersMessage = responseMessageFactory.createBadParametersMessage(message);
        return MultipartResponse.Builder.newInstance().header(badParametersMessage).build();
    }

    private MultipartResponse createErrorMultipartResponse(Message message) {
        RejectionMessage messageTypeNotSupportedMessage = responseMessageFactory.createMessageTypeNotSupportedMessage(message);
        return MultipartResponse.Builder.newInstance().header(messageTypeNotSupportedMessage).build();
    }
}
