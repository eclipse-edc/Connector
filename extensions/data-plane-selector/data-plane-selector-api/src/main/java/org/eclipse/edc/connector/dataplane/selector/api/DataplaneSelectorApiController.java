/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.api;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.function.Supplier;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v2/dataplanes")
public class DataplaneSelectorApiController implements DataplaneSelectorApi {

    private final DataPlaneSelectorService selectionService;
    private final TypeTransformerRegistry transformerRegistry;

    public DataplaneSelectorApiController(DataPlaneSelectorService selectionService, TypeTransformerRegistry transformerRegistry) {
        this.selectionService = selectionService;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    @POST
    @Path("select")
    public JsonObject find(JsonObject requestObject) {
        var request = transformerRegistry.transform(requestObject, SelectionRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var dpi = ofNullable(request.getStrategy())
                .map(strategy -> catchException(() -> selectionService.select(request.getSource(), request.getDestination(), strategy)))
                .orElseGet(() -> catchException(() -> selectionService.select(request.getSource(), request.getDestination())));

        if (dpi == null) {
            return null;
        }
        return transformerRegistry.transform(dpi, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @Override
    @POST
    public void addEntry(JsonObject jsonObject) {
        var instance = transformerRegistry.transform(jsonObject, DataPlaneInstance.class)
                .orElseThrow(InvalidRequestException::new);

        selectionService.addInstance(instance)
                .orElseThrow(exceptionMapper(DataPlaneInstance.class, instance.getId()));
    }

    @Override
    @GET
    public JsonArray getAll() {
        var instances = selectionService.getAll();
        return instances.stream()
                .map(i -> transformerRegistry.transform(i, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    private DataPlaneInstance catchException(Supplier<DataPlaneInstance> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }
}
