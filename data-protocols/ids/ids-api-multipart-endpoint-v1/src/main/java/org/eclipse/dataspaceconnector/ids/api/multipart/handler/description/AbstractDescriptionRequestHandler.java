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

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionResponseMessageUtil.createDescriptionResponseMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.MultipartResponseUtil.createBadParametersErrorMultipartResponse;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.MultipartResponseUtil.createNotFoundErrorMultipartResponse;

abstract class AbstractDescriptionRequestHandler<T, S> implements DescriptionRequestHandler {
    protected final String connectorId;
    protected final Monitor monitor;
    protected final IdsTransformerRegistry transformerRegistry;
    protected final IdsType targetIdsType;
    protected final Class<S> resultType;

    public AbstractDescriptionRequestHandler(
            @NotNull String connectorId,
            @NotNull Monitor monitor,
            @NotNull IdsTransformerRegistry transformerRegistry,
            @NotNull IdsType targetIdsType,
            @NotNull Class<S> resultType
    ) {
        this.connectorId = Objects.requireNonNull(connectorId);
        this.monitor = Objects.requireNonNull(monitor);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.targetIdsType = Objects.requireNonNull(targetIdsType);
        this.resultType = Objects.requireNonNull(resultType);
    }

    @Override
    public final MultipartResponse handle(
            @NotNull DescriptionRequestMessage descriptionRequestMessage,
            @NotNull Result<ClaimToken> verificationResult,
            @Nullable String payload) {
        Objects.requireNonNull(descriptionRequestMessage);

        URI uri = descriptionRequestMessage.getRequestedElement();
        if (uri == null) {
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        var result = transformerRegistry.transform(uri, IdsId.class);
        if (result.failed()) {
            monitor.warning(
                    String.format(
                            "Could not transform URI to IdsId: [%s]",
                            String.join(", ", result.getFailureMessages())
                    )
            );
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        IdsId idsId = result.getContent();
        if (Objects.requireNonNull(idsId).getType() != targetIdsType) {
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        T retrievedObject = retrieveObject(idsId, verificationResult);
        if (retrievedObject == null) {
            return createNotFoundErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        Result<S> transformResult = transformerRegistry.transform(retrievedObject, resultType);
        if (transformResult.failed()) {
            monitor.warning(
                    String.format(
                            "Could not transform %s to %S: [%s]",
                            retrievedObject.getClass().getSimpleName(),
                            resultType.getSimpleName(),
                            String.join(", ", transformResult.getFailureMessages())
                    )
            );
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        S handlerResult = transformResult.getContent();

        DescriptionResponseMessage descriptionResponseMessage = createDescriptionResponseMessage(connectorId, descriptionRequestMessage);

        return MultipartResponse.Builder.newInstance()
                .header(descriptionResponseMessage)
                .payload(handlerResult)
                .build();
    }

    protected abstract T retrieveObject(@NotNull IdsId idsId, @NotNull Result<ClaimToken> verificationResult);
}
