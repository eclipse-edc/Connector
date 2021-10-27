/*
 *  Copyright (c) 2021 BMW Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       BMW Group - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.assetindex;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.metadata.memory.InMemoryAssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexWriter;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/assets")
public class AssetIndexController {

    private final AssetIndex assetIndex;
    private final AssetIndexWriter assetIndexWriter;

    public AssetIndexController(AssetIndex assetIndex, AssetIndexWriter assetIndexWriter) {
        this.assetIndex = assetIndex;
        this.assetIndexWriter = assetIndexWriter;
    }

    @GET
    public Response getAssets() {
        Stream<Asset> assets = assetIndex.queryAssets(AssetSelectorExpression.SELECT_ALL);

        return Response.ok(assets.collect(toList())).build();
    }

    @POST
    public Response indexAsset(Map<String, Object> properties) {
        String id = Objects.toString(properties.get("id"));
        Asset asset = Asset.Builder.newInstance().id(id).properties(properties).build();
        assetIndexWriter.add(asset);
        return Response.ok().build();
    }
}
