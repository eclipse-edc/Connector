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
import de.fraunhofer.iais.eis.ResourceCatalog;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class DataCatalogDescriptionRequestHandler extends AbstractDescriptionRequestHandler {
    private final Monitor monitor;
    private final String connectorId;
    private final DataCatalogService dataCatalogService;
    private final TransformerRegistry transformerRegistry;

    public DataCatalogDescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull DataCatalogDescriptionRequestHandlerSettings dataCatalogDescriptionRequestHandlerSettings,
            @NotNull DataCatalogService dataCatalogService,
            @NotNull TransformerRegistry transformerRegistry) {
        super(monitor, transformerRegistry);
        this.monitor = Objects.requireNonNull(monitor);
        this.dataCatalogService = Objects.requireNonNull(dataCatalogService);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.connectorId = Objects.requireNonNull(dataCatalogDescriptionRequestHandlerSettings).getId();
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
        if (Objects.requireNonNull(idsId).getType() != IdsType.CATALOG) {
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        DataCatalog dataCatalog = dataCatalogService.getDataCatalog();
        if (dataCatalog == null) {
            return createNotFoundErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        TransformResult<ResourceCatalog> transformResult = transformerRegistry.transform(dataCatalog, ResourceCatalog.class);
        if (transformResult.hasProblems()) {
            monitor.warning(
                    String.format(
                            "Could not transform DataCatalog to ResourceCatalog: [%s]",
                            String.join(", ", result.getProblems())
                    )
            );
            return createBadParametersErrorMultipartResponse(connectorId, descriptionRequestMessage);
        }

        ResourceCatalog catalog = transformResult.getOutput();

        DescriptionResponseMessage descriptionResponseMessage = createDescriptionResponseMessage(connectorId, descriptionRequestMessage);

        return MultipartResponse.Builder.newInstance()
                .header(descriptionResponseMessage)
                .payload(catalog)
                .build();
    }
}
