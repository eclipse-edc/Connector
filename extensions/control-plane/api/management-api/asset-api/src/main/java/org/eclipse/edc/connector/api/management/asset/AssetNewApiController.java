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

package org.eclipse.edc.connector.api.management.asset;

import jakarta.json.JsonObject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseNewDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestWrapperDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.jsonld.util.JsonLdUtil;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/assets")
public class AssetNewApiController implements AssetNewApi {
    private final TypeTransformerRegistry transformerRegistry;
    private final AssetService service;
    private final DataAddressResolver dataAddressResolver;

    public AssetNewApiController(AssetService service, DataAddressResolver dataAddressResolver, TypeTransformerRegistry transformerRegistry) {
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.dataAddressResolver = dataAddressResolver;
    }

    @POST
    @Override
    public IdResponseDto createAsset(@Valid AssetEntryNewDto assetEntryDto) {
        var expandedJson = JsonLdUtil.expand(assetEntryDto.getAsset());
        if (expandedJson.size() == 0) throw new InvalidRequestException("Invalid asset!");

        var expandedJsonAsset = expandedJson.getJsonObject(0);
        var assetResult = transformerRegistry.transform(expandedJsonAsset, Asset.class);
        var dataAddressResult = transformerRegistry.transform(assetEntryDto.getDataAddress(), DataAddress.class);

        var result = assetResult.merge(dataAddressResult);
        if (result.failed()) {
            var errorMessages = result.getFailureMessages();
            throw new InvalidRequestException(errorMessages);
        }

        var dataAddress = dataAddressResult.getContent();
        var asset = assetResult.getContent();

        var resultContent = service.create(asset, dataAddress).orElseThrow(exceptionMapper(Asset.class, asset.getId()));

        return IdResponseDto.Builder.newInstance()
                .id(resultContent.getId())
                .createdAt(resultContent.getCreatedAt())
                .build();
    }

    @POST
    @Path("/request")
    @Override
    public List<AssetResponseNewDto> requestAssets(@Valid QuerySpecDto querySpecDto) {
        return queryAssets(ofNullable(querySpecDto).orElse(QuerySpecDto.Builder.newInstance().build()));
    }

    @GET
    @Path("{id}")
    @Override
    public AssetResponseNewDto getAsset(@PathParam("id") String id) {
        var obj = of(id)
                .map(it -> service.findById(id))
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));
        return Optional.ofNullable(expand(obj))
                .orElseThrow(() -> new EdcException("Error expanding asset"));
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAsset(@PathParam("id") String id) {
        service.delete(id).orElseThrow(exceptionMapper(Asset.class, id));
    }

    @PUT
    @Path("{assetId}")
    @Override
    public void updateAsset(@PathParam("assetId") String assetId, @Valid AssetUpdateRequestDto asset) {
        var wrapper = AssetUpdateRequestWrapperDto.Builder.newInstance().updateRequest(asset).assetId(assetId).build();
        var assetResult = transformerRegistry.transform(wrapper, Asset.class);
        if (assetResult.failed()) {
            throw new InvalidRequestException(assetResult.getFailureMessages());
        }
        service.update(assetResult.getContent())
                .orElseThrow(exceptionMapper(Asset.class, assetId));
    }

    @PUT
    @Path("{assetId}/dataaddress")
    @Override
    public void updateDataAddress(@PathParam("assetId") String assetId, @Valid DataAddressDto dataAddress) {
        var dataAddressResult = transformerRegistry.transform(dataAddress, DataAddress.class);
        if (dataAddressResult.failed()) {
            throw new InvalidRequestException(dataAddressResult.getFailureMessages());
        }
        service.update(assetId, dataAddressResult.getContent())
                .orElseThrow(exceptionMapper(DataAddress.class, assetId));
    }

    @GET
    @Path("{id}/dataaddress")
    @Override
    public DataAddressDto getAssetDataAddress(@PathParam("id") String id) {
        return of(id)
                .map(it -> dataAddressResolver.resolveForAsset(id))
                .map(it -> transformerRegistry.transform(it, DataAddressDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));
    }

    private List<AssetResponseNewDto> queryAssets(QuerySpecDto querySpecDto) {
        var transformationResult = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (transformationResult.failed()) {
            throw new InvalidRequestException(transformationResult.getFailureMessages());
        }

        var spec = transformationResult.getContent();

        try (var assets = service.query(spec).orElseThrow(exceptionMapper(QuerySpec.class, null))) {
            return assets
                    .map(it -> transformerRegistry.transform(it, JsonObject.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .map(this::expand)
                    .filter(Objects::nonNull)
                    .collect(toList());
        }
    }

    private AssetResponseNewDto expand(JsonObject jsonObject) {
        var expanded = JsonLdUtil.expand(jsonObject);
        if (expanded.size() > 0) {
            return AssetResponseNewDto.Builder.newInstance().asset(expanded.getJsonObject(0)).build();
        }
        return null;
    }


}
