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

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspaceconnector.api.ServiceResultHandler.mapToException;

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
    public void createAsset(@Valid AssetEntryDto assetEntryDto) {
        var assetResult = transformerRegistry.transform(assetEntryDto.getAsset(), Asset.class);
        var dataAddressResult = transformerRegistry.transform(assetEntryDto.getDataAddress(), DataAddress.class);

        if (assetResult.failed() || dataAddressResult.failed()) {
            var errorMessages = Stream.concat(assetResult.getFailureMessages().stream(), dataAddressResult.getFailureMessages().stream());
            throw new InvalidRequestException(errorMessages.collect(toList()));
        }

        var dataAddress = dataAddressResult.getContent();
        var asset = assetResult.getContent();

        var result = service.create(asset, dataAddress);

        if (result.succeeded()) {
            monitor.debug(format("Asset created %s", assetEntryDto.getAsset()));
        } else {
            throw mapToException(result, Asset.class, asset.getId());
        }
    }

    @GET
    @Override
    public List<AssetResponseDto> getAllAssets(@Valid @BeanParam QuerySpecDto querySpecDto) {
        var transformationResult = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (transformationResult.failed()) {
            throw new InvalidRequestException(transformationResult.getFailureMessages());
        }

        var spec = transformationResult.getContent();

        monitor.debug(format("get all Assets from %s", spec));

        var queryResult = service.query(spec);

        if (queryResult.failed()) {
            throw mapToException(queryResult, QuerySpec.class, null);
        }

        var assets = queryResult.getContent();

        return assets.stream()
                .map(it -> transformerRegistry.transform(it, AssetResponseDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toList());
    }

    @GET
    @Path("{id}")
    @Override
    public AssetResponseDto getAsset(@PathParam("id") String id) {
        monitor.debug(format("Attempting to return Asset with id %s", id));
        return Optional.of(id)
                .map(it -> service.findById(id))
                .map(it -> transformerRegistry.transform(it, AssetResponseDto.class))
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
            throw mapToException(result, Asset.class, id);
        }
    }
}
