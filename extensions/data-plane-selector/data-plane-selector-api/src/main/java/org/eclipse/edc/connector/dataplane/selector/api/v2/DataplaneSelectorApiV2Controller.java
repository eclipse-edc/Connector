/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.api.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.dataplane.selector.api.model.SelectionRequest;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.time.Clock;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v2/dataplanes")
public class DataplaneSelectorApiV2Controller implements DataplaneSelectorApiV2 {

    private final DataPlaneSelectorService selectionService;
    private final TypeTransformerRegistry transformerRegistry;

    private final JsonObjectValidatorRegistry validatorRegistry;

    private final Clock clock;
    private final Monitor monitor;

    public DataplaneSelectorApiV2Controller(DataPlaneSelectorService selectionService, TypeTransformerRegistry transformerRegistry,
                                            JsonObjectValidatorRegistry validatorRegistry, Clock clock, Monitor monitor) {
        this.selectionService = selectionService;
        this.transformerRegistry = transformerRegistry;
        this.validatorRegistry = validatorRegistry;
        this.clock = clock;
        this.monitor = monitor;
    }

    @Override
    @POST
    @Path("select")
    public JsonObject selectDataPlaneInstanceV2(JsonObject requestObject) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        var request = transformerRegistry.transform(requestObject, SelectionRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var selection = selectionService.select(request.getSource(), request.getTransferType(), request.getStrategy());

        if (selection.failed()) {
            return null;
        }
        return transformerRegistry.transform(selection.getContent(), JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @Override
    @POST
    public JsonObject addDataPlaneInstanceV2(JsonObject jsonObject) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        validatorRegistry.validate(DATAPLANE_INSTANCE_TYPE, jsonObject).orElseThrow(ValidationFailureException::new);

        var instance = transformerRegistry.transform(jsonObject, DataPlaneInstance.class)
                .orElseThrow(InvalidRequestException::new);

        selectionService.addInstance(instance)
                .orElseThrow(exceptionMapper(DataPlaneInstance.class, instance.getId()));

        var idResponse = IdResponse.Builder.newInstance()
                .id(instance.getId())
                .createdAt(clock.millis())
                .build();

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @Override
    @GET
    public JsonArray getAllDataPlaneInstancesV2() {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        var instances = selectionService.getAll().orElseThrow(exceptionMapper(DataPlaneInstance.class));
        return instances.stream()
                .map(i -> transformerRegistry.transform(i, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

}
