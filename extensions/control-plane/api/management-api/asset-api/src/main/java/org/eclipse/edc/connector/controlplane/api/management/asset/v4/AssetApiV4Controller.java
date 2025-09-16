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

package org.eclipse.edc.connector.controlplane.api.management.asset.v4;

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
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_CATALOG_ASSET_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/assets")
public class AssetApiV4Controller extends BaseAssetApiController implements AssetApiV4 {

    public AssetApiV4Controller(AssetService service, TypeTransformerRegistry transformerRegistry,
                                Monitor monitor, JsonObjectValidatorRegistry validator, SingleParticipantContextSupplier participantContextSupplier) {
        super(transformerRegistry, service, monitor, validator, participantContextSupplier);
    }

    @POST
    @Override
    public JsonObject createAssetV4(@SchemaType({EDC_ASSET_TYPE_TERM, EDC_CATALOG_ASSET_TYPE_TERM}) JsonObject asset) {
        return createAsset(asset);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestAssetsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        return requestAssets(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getAssetV4(@PathParam("id") String id) {
        return getAsset(id);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAssetV4(@PathParam("id") String id) {
        removeAsset(id);
    }

    @PUT
    @Override
    public void updateAssetV4(@SchemaType({EDC_ASSET_TYPE_TERM, EDC_CATALOG_ASSET_TYPE_TERM}) JsonObject asset) {
        updateAsset(asset);
    }
}
