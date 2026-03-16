/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.api.query;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.federatedcatalog.util.FederatedCatalogUtil;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

public abstract class BaseCatalogsApiController {

    private final QueryService queryService;
    private final TypeTransformerRegistry transformerRegistry;

    public BaseCatalogsApiController(QueryService queryService, TypeTransformerRegistry transformerRegistry) {
        this.queryService = queryService;
        this.transformerRegistry = transformerRegistry;
    }

    public JsonArray requestCatalogs(JsonObject querySpecJson, boolean flatten) {
        var querySpec = querySpecJson == null
                ? QuerySpec.none()
                : transformerRegistry.transform(querySpecJson, QuerySpec.class)
                        .orElseThrow(InvalidRequestException::new);

        return queryService.getCatalog(querySpec)
                .orElseThrow(exceptionMapper(Catalog.class))
                .stream()
                .map(catalog -> flatten ? FederatedCatalogUtil.flatten(catalog) : catalog)
                .map(catalog -> transformerRegistry.transform(catalog, JsonObject.class))
                .filter(Result::succeeded)
                .map(AbstractResult::getContent)
                .collect(toJsonArray());
    }
}
