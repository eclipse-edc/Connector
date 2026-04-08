/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.dataaddress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataPlaneProtocolInUse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;
import java.util.Optional;
import java.util.function.Supplier;

import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_SECRET;

public class VaultDataAddressStore implements DataAddressStore {

    private final Vault vault;
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final JsonLd jsonLd;
    private final DataPlaneProtocolInUse dataPlaneProtocolInUse;
    private final Supplier<ObjectMapper> objectMapperSupplier;

    public VaultDataAddressStore(Vault vault, TypeTransformerRegistry typeTransformerRegistry, JsonLd jsonLd,
                                 DataPlaneProtocolInUse dataPlaneProtocolInUse, Supplier<ObjectMapper> objectMapperSupplier) {
        this.vault = vault;
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.jsonLd = jsonLd;
        this.dataPlaneProtocolInUse = dataPlaneProtocolInUse;
        this.objectMapperSupplier = objectMapperSupplier;
    }

    private static Result<JsonObject> readJsonObject(String json) {
        try {
            var jsonObject = Json.createReader(new StringReader(json)).readObject();
            return Result.success(jsonObject);
        } catch (Exception e) {
            return Result.failure("Cannot deserialize data address");
        }
    }

    @Override
    public StoreResult<Void> store(DataAddress dataAddress, TransferProcess transferProcess) {
        var alias = "transfer-process-" + transferProcess.getId() + "-data-address";
        return toJson(dataAddress).map(Object::toString)
                .compose(json -> vault.storeSecret(transferProcess.getParticipantContextId(), alias, json))
                .flatMap(this::toStoreResult)
                .onSuccess(o -> {
                    transferProcess.setDataAddressAlias(alias);
                    if (dataPlaneProtocolInUse.isLegacy()) {
                        transferProcess.updateDestination(dataAddress);
                    } else {
                        transferProcess.updateDestination(null);
                    }
                });
    }

    private Result<String> toJson(DataAddress dataAddress) {
        if (dataPlaneProtocolInUse.isLegacy()) {
            return typeTransformerRegistry.transform(dataAddress, JsonObject.class)
                    .compose(jsonLd::expand)
                    .map(Object::toString);
        } else {
            try {
                return Result.success(objectMapperSupplier.get().writeValueAsString(dataAddress));
            } catch (JsonProcessingException e) {
                return Result.failure(e.getMessage());
            }
        }

    }

    private Result<DataAddress> fromJson(String json) {
        return readJsonObject(json).compose(jsonObject -> {
            if (dataPlaneProtocolInUse.isLegacy()) {
                return typeTransformerRegistry.transform(jsonObject, DataAddress.class);
            } else {
                try {
                    return Result.success(objectMapperSupplier.get().readValue(json, DataAddress.class));
                } catch (JsonProcessingException e) {
                    return Result.failure(e.getMessage());
                }
            }
        });
    }

    @Override
    public StoreResult<DataAddress> resolve(TransferProcess transferProcess) {
        var dataAddressAlias = transferProcess.getDataAddressAlias();
        if (dataAddressAlias == null) {
            var originalDestination = transferProcess.getDataDestination();
            if (originalDestination == null) {
                return StoreResult.notFound("No data address found for transfer process " + transferProcess.getId());
            }

            var dataDestination = Optional.ofNullable(originalDestination)
                    .map(DataAddress::getKeyName)
                    .map(key -> vault.resolveSecret(transferProcess.getParticipantContextId(), key))
                    .map(secret -> originalDestination.toBuilder().property(EDC_DATA_ADDRESS_SECRET, secret).build())
                    .orElse(originalDestination);

            return StoreResult.success(dataDestination);
        }

        var json = vault.resolveSecret(transferProcess.getParticipantContextId(), dataAddressAlias);
        return fromJson(json).flatMap(this::toStoreResult);
    }

    @Override
    public StoreResult<Void> remove(TransferProcess transferProcess) {
        var dataAddressAlias = transferProcess.getDataAddressAlias();
        if (dataAddressAlias == null) {
            return StoreResult.success();
        }

        var result = vault.deleteSecret(transferProcess.getParticipantContextId(), dataAddressAlias);
        if (result.succeeded()) {
            transferProcess.setDataAddressAlias(null);
            return StoreResult.success();
        } else {
            return StoreResult.generalError(result.getFailureDetail());
        }
    }

    private <T> @NotNull StoreResult<T> toStoreResult(Result<T> result) {
        if (result.succeeded()) {
            return StoreResult.success(result.getContent());
        } else {
            return StoreResult.generalError(result.getFailureDetail());
        }
    }
}
