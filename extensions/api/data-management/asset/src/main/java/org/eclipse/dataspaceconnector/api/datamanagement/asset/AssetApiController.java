/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - Added initiate-transfer endpoint
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
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
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/assets")
public class AssetApiController implements AssetApi {

    private final Monitor monitor;
    private final AssetService service;
    private final DtoTransformerRegistry transformerRegistry;

    public AssetApiController(Monitor monitor, AssetService service, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @Override
    public void createAsset(AssetEntryDto assetEntryDto) {
        var assetResult = transformerRegistry.transform(assetEntryDto.getAsset(), Asset.class);
        var dataAddressResult = transformerRegistry.transform(assetEntryDto.getDataAddress(), DataAddress.class);

        if (assetResult.failed() || dataAddressResult.failed()) {
            throw new IllegalArgumentException("Request is not well formatted");
        }

        var dataAddress = dataAddressResult.getContent();
        var asset = assetResult.getContent();

        var result = service.create(asset, dataAddress);

        if (result.succeeded()) {
            monitor.debug(format("Asset created %s", assetEntryDto.getAsset()));
        } else {
            handleFailedResult(result, asset.getId());
        }
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

        return service.query(spec).stream()
                .map(it -> transformerRegistry.transform(it, AssetDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    @Override
    public AssetDto getAsset(@PathParam("id") String id) {
        monitor.debug(format("Attempting to return Asset with id %s", id));
        return Optional.of(id)
                .map(it -> service.findById(id))
                .map(it -> transformerRegistry.transform(it, AssetDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAsset(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete Asset with id %s", id));
        var result = service.delete(id);
        if (result.succeeded()) {
            monitor.debug(format("Asset deleted %s", id));
        } else {
            handleFailedResult(result, id);
        }
    }

    private void handleFailedResult(ServiceResult<Asset> result, String id) {
        switch (result.reason()) {
            case NOT_FOUND: throw new ObjectNotFoundException(Asset.class, id);
            case CONFLICT: throw new ObjectExistsException(Asset.class, id);
            default: throw new EdcException("unexpected error");
        }
    }

}
