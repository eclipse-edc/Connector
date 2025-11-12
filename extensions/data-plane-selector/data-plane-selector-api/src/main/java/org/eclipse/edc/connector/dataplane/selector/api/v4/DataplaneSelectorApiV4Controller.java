/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.api.v4;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/v4beta/dataplanes")
public class DataplaneSelectorApiV4Controller implements DataplaneSelectorApiV4 {

    private final DataPlaneSelectorService selectionService;
    private final TypeTransformerRegistry transformerRegistry;

    public DataplaneSelectorApiV4Controller(DataPlaneSelectorService selectionService, TypeTransformerRegistry transformerRegistry) {
        this.selectionService = selectionService;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    @GET
    public JsonArray getAllDataPlaneInstancesV4() {
        var instances = selectionService.getAll().orElseThrow(exceptionMapper(DataPlaneInstance.class));
        return instances.stream()
                .map(i -> transformerRegistry.transform(i, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

}
