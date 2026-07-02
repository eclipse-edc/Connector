/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile.v5;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.service.DataspaceProfileService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/dataspaceprofiles")
public class DataspaceProfileApiV5Controller implements DataspaceProfileApiV5 {

    private final DataspaceProfileService service;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public DataspaceProfileApiV5Controller(DataspaceProfileService service, TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @RequiredScope("management-api:profiles:write")
    @Override
    public JsonObject createProfileV5(@SchemaType(DATASPACE_PROFILE_CONTEXT_TYPE_TERM) JsonObject request) {

        var profile = transformerRegistry.transform(request, DataspaceProfile.class)
                .orElseThrow(InvalidRequestException::new);

        var created = service.create(profile)
                .onSuccess(p -> monitor.debug(format("Dataspace profile created %s", p.getName())))
                .orElseThrow(exceptionMapper(DataspaceProfile.class, profile.getName()));

        return transformerRegistry.transform(created, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @Path("request")
    @RequiredScope("management-api:profiles:read")
    @Override
    public JsonArray queryProfilesV5(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.search(querySpec).orElseThrow(exceptionMapper(QuerySpec.class, null))
                .stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("{name}")
    @RequiredScope("management-api:profiles:read")
    @Override
    public JsonObject getProfileV5(@PathParam("name") String name) {

        var profile = service.findById(name);
        if (profile == null) {
            throw new ObjectNotFoundException(DataspaceProfile.class, name);
        }

        return transformerRegistry.transform(profile, JsonObject.class)
                .orElseThrow(InvalidRequestException::new);
    }

    @DELETE
    @Path("{name}")
    @RequiredScope("management-api:profiles:write")
    @Override
    public void deleteProfileV5(@PathParam("name") String name) {

        service.deleteById(name)
                .onSuccess(p -> monitor.debug(format("Dataspace profile deleted %s", p.getName())))
                .orElseThrow(exceptionMapper(DataspaceProfile.class, name));
    }
}
