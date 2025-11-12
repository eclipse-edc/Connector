/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.edr.v4;


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
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4beta/edrs")
public class EdrCacheApiV4Controller extends BaseEdrCacheApiController implements EdrCacheApiV4 {
    public EdrCacheApiV4Controller(EndpointDataReferenceStore edrStore, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(edrStore, transformerRegistry, validator, monitor);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestEdrEntriesV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        return requestEdrEntries(querySpecJson);
    }

    @GET
    @Path("{transferProcessId}/dataaddress")
    @Override
    public JsonObject getEdrEntryDataAddressV4(@PathParam("transferProcessId") String transferProcessId) {
        return getEdrEntryDataAddress(transferProcessId);
    }

    @DELETE
    @Path("{transferProcessId}")
    @Override
    public void removeEdrEntryV4(@PathParam("transferProcessId") String transferProcessId) {
        removeEdrEntry(transferProcessId);
    }
}
