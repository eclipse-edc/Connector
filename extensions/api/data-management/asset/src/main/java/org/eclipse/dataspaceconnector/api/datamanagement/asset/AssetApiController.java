/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 *    Microsoft Corporation - Added initiate-transfer endpoint
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/assets")
public class AssetApiController implements AssetApi {

    private final Monitor monitor;

    public AssetApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    @POST
    @Override
    public void createAsset(AssetEntryDto assetEntry) {
        monitor.debug(format("Asset created %s", assetEntry.getAsset()));
    }

    @GET
    @Override
    public List<AssetDto> getAllAssets(@QueryParam("offset") Integer offset,
                                       @QueryParam("limit") Integer limit,
                                       @QueryParam("filter") String filterExpression,
                                       @QueryParam("sort") SortOrder sortOrder,
                                       @QueryParam("sortField") String sortField) {

        var spec = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .sortField(sortField)
                .filter(filterExpression)
                .sortOrder(sortOrder).build();

        monitor.debug(format("get all Assets from %s", spec));

        return Collections.emptyList();
    }

    @GET
    @Path("{id}")
    @Override
    public AssetDto getAsset(@PathParam("id") String id) {
        monitor.debug(format("Attempting to return Asset with id %s", id));
        return null;
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAsset(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete Asset with id %s", id));
    }

    @POST
    @Path("{id}/transfer")
    @Override
    public String initiateTransfer(@PathParam("id") String assetId, TransferRequestDto transferRequest) {

        if (StringUtils.isNullOrBlank(assetId)) {
            throw new IllegalArgumentException("Asset ID not valid");
        }

        if (!isValid(transferRequest)) {
            throw new IllegalArgumentException("Transfer request body not valid");
        }
        monitor.debug("Starting transfer for asset " + assetId + "to " + transferRequest.getDataDestination());
        return "not-implemented"; //will be the transfer process id
    }

    private boolean isValid(TransferRequestDto transferRequest) {
        return !StringUtils.isNullOrBlank(transferRequest.getConnectorAddress()) &&
                !StringUtils.isNullOrBlank(transferRequest.getContractId()) &&
                !StringUtils.isNullOrBlank(transferRequest.getProtocol()) &&
                transferRequest.getDataDestination() != null;
    }
}
