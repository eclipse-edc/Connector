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
import de.fraunhofer.iais.eis.Representation;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class RepresentationDescriptionRequestHandler extends AbstractDescriptionRequestHandler implements DescriptionRequestHandler {
    private final Monitor monitor;
    private final String connectorId;
    private final AssetIndex assetIndex;
    private final TransformerRegistry transformerRegistry;

    public RepresentationDescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull RepresentationDescriptionRequestHandlerSettings representationDescriptionRequestHandlerSettings,
            @NotNull AssetIndex assetIndex,
            @NotNull TransformerRegistry transformerRegistry) {
        super(monitor, transformerRegistry);
        this.monitor = Objects.requireNonNull(monitor);
        this.assetIndex = Objects.requireNonNull(assetIndex);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.connectorId = Objects.requireNonNull(representationDescriptionRequestHandlerSettings).getId();
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
        if (Objects.requireNonNull(idsId).getType() != IdsType.REPRESENTATION) {
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        Asset asset = assetIndex.findById(idsId.getValue());
        if (asset == null) {
            return createNotFoundErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        TransformResult<Representation> transformResult = transformerRegistry.transform(asset, Representation.class);
        if (transformResult.hasProblems()) {
            monitor.warning(
                    String.format(
                            "Could not transform Asset to Representation: [%s]",
                            String.join(", ", transformResult.getProblems())
                    )
            );
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        Representation representation = transformResult.getOutput();

        DescriptionResponseMessage descriptionResponseMessage = createDescriptionResponseMessage(connectorId, descriptionRequestMessage);

        return MultipartResponse.Builder.newInstance()
                .header(descriptionResponseMessage)
                .payload(representation)
                .build();
    }
}
