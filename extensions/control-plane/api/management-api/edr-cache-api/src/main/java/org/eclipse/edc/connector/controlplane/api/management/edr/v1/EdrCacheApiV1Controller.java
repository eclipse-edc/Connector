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

package org.eclipse.edc.connector.controlplane.api.management.edr.v1;


import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.api.management.edr.BaseEdrCacheApiController;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/edrs")
public class EdrCacheApiV1Controller extends BaseEdrCacheApiController implements EdrCacheApiV1 {
    public EdrCacheApiV1Controller(EndpointDataReferenceStore edrStore, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(edrStore, transformerRegistry, validator, monitor);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestEdrEntriesV1(JsonObject querySpecJson) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        return requestEdrEntries(querySpecJson);
    }

    @GET
    @Path("{transferProcessId}/dataaddress")
    @Override
    public JsonObject getEdrEntryDataAddressV1(@PathParam("transferProcessId") String transferProcessId) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        return getEdrEntryDataAddress(transferProcessId);
    }

    @DELETE
    @Path("{transferProcessId}")
    @Override
    public void removeEdrEntryV1(@PathParam("transferProcessId") String transferProcessId) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        removeEdrEntry(transferProcessId);
    }
}
