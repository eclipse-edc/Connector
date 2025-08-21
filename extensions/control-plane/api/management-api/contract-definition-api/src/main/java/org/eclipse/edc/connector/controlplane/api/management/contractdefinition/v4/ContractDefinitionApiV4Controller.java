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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v4;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.api.management.contractdefinition.BaseContractDefinitionApiController;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/contractdefinitions")
public class ContractDefinitionApiV4Controller extends BaseContractDefinitionApiController implements ContractDefinitionApiV4 {
    public ContractDefinitionApiV4Controller(TypeTransformerRegistry transformerRegistry, ContractDefinitionService service, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(transformerRegistry, service, monitor, validatorRegistry);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryContractDefinitionsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        return queryContractDefinitions(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getContractDefinitionV4(@PathParam("id") String id) {
        return getContractDefinition(id);
    }

    @POST
    @Override
    public JsonObject createContractDefinitionV4(@SchemaType(CONTRACT_DEFINITION_TYPE_TERM) JsonObject createObject) {
        return createContractDefinition(createObject);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deleteContractDefinitionV4(@PathParam("id") String id) {
        deleteContractDefinition(id);
    }

    @PUT
    @Override
    public void updateContractDefinitionV4(@SchemaType(CONTRACT_DEFINITION_TYPE_TERM) JsonObject updateObject) {
        updateContractDefinition(updateObject);
    }
}
