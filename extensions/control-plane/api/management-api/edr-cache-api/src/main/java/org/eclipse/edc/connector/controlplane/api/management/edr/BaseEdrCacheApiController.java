/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.edr;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class BaseEdrCacheApiController {
    protected final EndpointDataReferenceStore edrStore;
    protected final TypeTransformerRegistry transformerRegistry;
    protected final JsonObjectValidatorRegistry validator;
    protected final Monitor monitor;

    public BaseEdrCacheApiController(EndpointDataReferenceStore edrStore, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        this.edrStore = edrStore;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    public JsonArray requestEdrEntries(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return edrStore.query(querySpec)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("{transferProcessId}/dataaddress")
    public JsonObject getEdrEntryDataAddress(@PathParam("transferProcessId") String transferProcessId) {
        var dataAddress = edrStore.resolveByTransferProcess(transferProcessId)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class, transferProcessId));

        return transformerRegistry.transform(dataAddress, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));


    }

    @DELETE
    @Path("{transferProcessId}")
    public void removeEdrEntry(@PathParam("transferProcessId") String transferProcessId) {
        edrStore.delete(transferProcessId)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class, transferProcessId));
    }
}
