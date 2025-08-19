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

package org.eclipse.edc.connector.controlplane.api.management.asset.v3;

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
import org.eclipse.edc.connector.controlplane.api.management.asset.BaseAssetApiController;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/assets")
public class AssetApiV3Controller extends BaseAssetApiController implements AssetApiV3 {

    public AssetApiV3Controller(AssetService service, TypeTransformerRegistry transformerRegistry,
                                Monitor monitor, JsonObjectValidatorRegistry validator) {
        super(transformerRegistry, service, monitor, validator);
    }

    @POST
    @Override
    public JsonObject createAssetV3(JsonObject asset) {
        return createAsset(asset);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestAssetsV3(JsonObject querySpecJson) {
        return requestAssets(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getAssetV3(@PathParam("id") String id) {
        return getAsset(id);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAssetV3(@PathParam("id") String id) {
        removeAsset(id);
    }

    @PUT
    @Override
    public void updateAssetV3(JsonObject asset) {
        updateAsset(asset);
    }
}
