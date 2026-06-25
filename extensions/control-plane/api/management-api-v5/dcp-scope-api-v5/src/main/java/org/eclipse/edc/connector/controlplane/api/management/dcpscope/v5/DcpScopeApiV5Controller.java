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

package org.eclipse.edc.connector.controlplane.api.management.dcpscope.v5;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/dcpscopes")
public class DcpScopeApiV5Controller implements DcpScopeApiV5 {

    private final DcpScopeRegistry scopeRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public DcpScopeApiV5Controller(DcpScopeRegistry scopeRegistry, TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.scopeRegistry = scopeRegistry;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @RequiredScope("management-api:admin")
    @Override
    public JsonObject createDcpScopeV5(@SchemaType(DCP_SCOPE_TYPE_TERM) JsonObject request) {
        var scope = transformerRegistry.transform(request, DcpScope.class)
                .orElseThrow(InvalidRequestException::new);

        scopeRegistry.create(scope)
                .orElseThrow(exceptionMapper(DcpScope.class, scope.getId()));

        var idResponse = IdResponse.Builder.newInstance()
                .id(scope.getId())
                .build();

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @PUT
    @Path("{id}")
    @RequiredScope("management-api:admin")
    @Override
    public void updateDcpScopeV5(@PathParam("id") String id, @SchemaType(DCP_SCOPE_TYPE_TERM) JsonObject request) {
        var scope = transformerRegistry.transform(request, DcpScope.class)
                .orElseThrow(InvalidRequestException::new);

        scopeRegistry.update(scope)
                .orElseThrow(exceptionMapper(DcpScope.class, id));
    }

    @DELETE
    @Path("{id}")
    @RequiredScope("management-api:admin")
    @Override
    public void deleteDcpScopeV5(@PathParam("id") String id) {
        scopeRegistry.remove(id)
                .orElseThrow(exceptionMapper(DcpScope.class, id));
    }

    @POST
    @Path("/request")
    @RequiredScope("management-api:admin")
    @Override
    public JsonArray queryDcpScopesV5(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return scopeRegistry.query(querySpec)
                .orElseThrow(exceptionMapper(DcpScope.class, null)).stream()
                .map(scope -> transformerRegistry.transform(scope, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }
}
