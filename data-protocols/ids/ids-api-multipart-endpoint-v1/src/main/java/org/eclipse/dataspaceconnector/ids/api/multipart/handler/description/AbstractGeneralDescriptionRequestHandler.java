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
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionResponseMessageUtil.createDescriptionResponseMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.MultipartResponseUtil.createBadParametersErrorMultipartResponse;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.MultipartResponseUtil.createNotFoundErrorMultipartResponse;

abstract class AbstractGeneralDescriptionRequestHandler<T, S> implements DescriptionRequestHandler {
    protected final String connectorId;
    protected final Monitor monitor;
    protected final TransformerRegistry transformerRegistry;
    protected final IdsType targetIdsType;
    protected final Class<S> resultType;

    public AbstractGeneralDescriptionRequestHandler(
            @NotNull String connectorId,
            @NotNull Monitor monitor,
            @NotNull TransformerRegistry transformerRegistry,
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
    public MultipartResponse handle(@NotNull DescriptionRequestMessage descriptionRequestMessage, @Nullable String payload) {
        Objects.requireNonNull(descriptionRequestMessage);

        URI uri = descriptionRequestMessage.getRequestedElement();
        if (uri == null) {
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        var result = transformerRegistry.transform(uri, IdsId.class);
        if (result.hasProblems()) {
            monitor.warning(
                    String.format(
                            "Could not transform URI to IdsId: [%s]",
                            String.join(", ", result.getProblems())
                    )
            );
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        IdsId idsId = result.getOutput();
        if (Objects.requireNonNull(idsId).getType() != targetIdsType) {
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        T suppliedObject = retrieveObject(idsId);
        if (suppliedObject == null) {
            return createNotFoundErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        TransformResult<S> transformResult = transformerRegistry.transform(suppliedObject, resultType);
        if (transformResult.hasProblems()) {
            monitor.warning(
                    String.format(
                            "Could not transform %s to %S: [%s]",
                            suppliedObject.getClass().getSimpleName(),
                            resultType.getSimpleName(),
                            String.join(", ", transformResult.getProblems())
                    )
            );
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        S handlerResult = transformResult.getOutput();

        DescriptionResponseMessage descriptionResponseMessage = createDescriptionResponseMessage(connectorId, descriptionRequestMessage);

        return MultipartResponse.Builder.newInstance()
                .header(descriptionResponseMessage)
                .payload(handlerResult)
                .build();
    }

    protected abstract T retrieveObject(IdsId idsId);
}
