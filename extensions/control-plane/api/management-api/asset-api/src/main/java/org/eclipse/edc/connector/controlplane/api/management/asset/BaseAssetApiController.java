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

package org.eclipse.edc.connector.controlplane.api.management.asset;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.util.Optional.of;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

public abstract class BaseAssetApiController {
    protected final TypeTransformerRegistry transformerRegistry;
    protected final AssetService service;
    protected final Monitor monitor;
    protected final JsonObjectValidatorRegistry validator;
    private final SingleParticipantContextSupplier participantContextSupplier;

    public BaseAssetApiController(TypeTransformerRegistry transformerRegistry, AssetService service, Monitor monitor,
                                  JsonObjectValidatorRegistry validator, SingleParticipantContextSupplier participantContextSupplier) {
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.monitor = monitor;
        this.validator = validator;
        this.participantContextSupplier = participantContextSupplier;
    }

    public JsonObject createAsset(JsonObject assetJson) {
        validator.validate(EDC_ASSET_TYPE, assetJson).orElseThrow(ValidationFailureException::new);
        var asset = transformerRegistry.transform(assetJson, Asset.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextSupplier.get().participantContextId())
                .build();

        var idResponse = service.create(asset)
                .map(a -> IdResponse.Builder.newInstance()
                        .id(a.getId())
                        .createdAt(a.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(Asset.class, asset.getId()));

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public JsonArray requestAssets(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.search(querySpec).orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    public JsonObject getAsset(String id) {
        var asset = of(id)
                .map(it -> service.findById(id))
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, id));

        return transformerRegistry.transform(asset, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));

    }

    public void removeAsset(String id) {
        service.delete(id).orElseThrow(exceptionMapper(Asset.class, id));
    }

    public void updateAsset(JsonObject assetJson) {
        validator.validate(EDC_ASSET_TYPE, assetJson).orElseThrow(ValidationFailureException::new);

        var assetResult = transformerRegistry.transform(assetJson, Asset.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(assetResult)
                .orElseThrow(exceptionMapper(Asset.class, assetResult.getId()));
    }
}
