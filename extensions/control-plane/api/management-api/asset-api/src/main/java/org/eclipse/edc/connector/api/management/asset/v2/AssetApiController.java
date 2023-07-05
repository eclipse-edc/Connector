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

package org.eclipse.edc.connector.api.management.asset.v2;

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
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Optional.of;
import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_TYPE;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/assets")
@Deprecated(since = "0.1.3")
public class AssetApiController implements AssetApi {
    private final TypeTransformerRegistry transformerRegistry;
    private final AssetService service;
    private final DataAddressResolver dataAddressResolver;
    private final Monitor monitor;
    private final JsonObjectValidatorRegistry validator;

    public AssetApiController(AssetService service, DataAddressResolver dataAddressResolver, TypeTransformerRegistry transformerRegistry,
                              Monitor monitor, JsonObjectValidatorRegistry validator) {
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.dataAddressResolver = dataAddressResolver;
        this.monitor = monitor;
        this.validator = validator;
    }

    @POST
    @Override
    public JsonObject createAsset(JsonObject assetEntryDto) {
        logDeprecationWarning();
        validator.validate(EDC_ASSET_ENTRY_DTO_TYPE, assetEntryDto).orElseThrow(ValidationFailureException::new);

        var assetEntry = transformerRegistry.transform(assetEntryDto, AssetEntryNewDto.class)
                .orElseThrow(InvalidRequestException::new);

        var dto = service.create(assetEntry.getAsset(), assetEntry.getDataAddress())
                .map(a -> IdResponseDto.Builder.newInstance()
                        .id(a.getId())
                        .createdAt(a.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(Asset.class, assetEntry.getAsset().getId()));

        return transformerRegistry.transform(dto, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestAssets(JsonObject querySpecDto) {
        logDeprecationWarning();
        QuerySpec querySpec;
        if (querySpecDto == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecDto).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecDto, QuerySpecDto.class)
                    .compose(dto -> transformerRegistry.transform(dto, QuerySpec.class))
                    .orElseThrow(InvalidRequestException::new);
        }

        try (var assets = service.query(querySpec).orElseThrow(exceptionMapper(QuerySpec.class, null))) {
            return assets
                    .map(it -> transformerRegistry.transform(it, JsonObject.class))
                    .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toJsonArray());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getAsset(@PathParam("id") String id) {
        logDeprecationWarning();
        var asset = of(id)
                .map(it -> service.findById(id))
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));

        return transformerRegistry.transform(asset, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));

    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeAsset(@PathParam("id") String id) {
        logDeprecationWarning();
        service.delete(id).orElseThrow(exceptionMapper(Asset.class, id));
    }

    @PUT
    @Override
    public void updateAsset(JsonObject assetJsonObject) {
        logDeprecationWarning();
        // validation removed because now the asset validation requires the dataAddress field
        // validator.validate(EDC_ASSET_TYPE, assetJsonObject).orElseThrow(ValidationFailureException::new);

        var assetResult = transformerRegistry.transform(assetJsonObject, Asset.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(assetResult)
                .orElseThrow(exceptionMapper(Asset.class, assetResult.getId()));
    }

    @PUT
    @Path("{assetId}/dataaddress")
    @Override
    public void updateDataAddress(@PathParam("assetId") String assetId, JsonObject dataAddressJson) {
        logDeprecationWarning();
        validator.validate(EDC_DATA_ADDRESS_TYPE, dataAddressJson).orElseThrow(ValidationFailureException::new);

        var dataAddressResult = transformerRegistry.transform(dataAddressJson, DataAddress.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(assetId, dataAddressResult)
                .orElseThrow(exceptionMapper(DataAddress.class, assetId));
    }

    @GET
    @Path("{id}/dataaddress")
    @Override
    public JsonObject getAssetDataAddress(@PathParam("id") String id) {
        logDeprecationWarning();
        return of(id)
                .map(it -> dataAddressResolver.resolveForAsset(id))
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));
    }

    private void logDeprecationWarning() {
        monitor.warning("the /v2/assets endpoint have been deprecated, please switch to the new /v3/assets");
    }

}
