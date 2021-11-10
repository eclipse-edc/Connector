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

import de.fraunhofer.iais.eis.ResourceCatalog;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

public class DataCatalogDescriptionRequestHandler extends AbstractDescriptionRequestHandler<DataCatalog, ResourceCatalog> {
    private final DataCatalogService dataCatalogService;

    public DataCatalogDescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull DataCatalogService dataCatalogService,
            @NotNull TransformerRegistry transformerRegistry) {
        super(
                connectorId,
                monitor,
                transformerRegistry,
                IdsType.CATALOG,
                ResourceCatalog.class
        );
        this.dataCatalogService = dataCatalogService;
    }

    protected DataCatalog retrieveObject(@NotNull IdsId idsId, @NotNull VerificationResult verificationResult) {
        return dataCatalogService.getDataCatalog(verificationResult);
    }
}
