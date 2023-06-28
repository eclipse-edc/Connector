/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi;

import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Handles contract ID generation for contract offers and agreements originating in an EDC runtime.
 * Ids are architected to allow the contract definition which generated the contract to be de-referenced.
 * The id format follows the following scheme: <code>[definition-id]:[asset-id]:[UUID]</code>
 */
public final class ContractId {

    private static final String DELIMITER = ":";
    private static final int DEFINITION_ID_PART = 0;
    private static final int ASSET_ID_PART = 1;
    private static final int UUID_PART = 2;
    private final String definitionId;
    private final String assetId;
    private final String uuid;

    private ContractId(String definitionId, String assetId, String uuid) {
        this.definitionId = definitionId;
        this.assetId = assetId;
        this.uuid = uuid;
    }

    public static ContractId create(String definitionId, String assetId) {
        return new ContractId(definitionId, assetId, UUID.randomUUID().toString());
    }

    /**
     * Returns a new id given the definition part
     *
     * @param definitionPart the part that will be used as prefix of the id
     * @param assetId        The ID of the asset that is contained in the offer
     * @return a {@link String} that represent the contract id
     * @deprecated please use {@link #create(String, String)}
     */
    @NotNull
    @Deprecated(since = "0.1.2", forRemoval = true)
    public static String createContractId(String definitionPart, String assetId) {
        return create(definitionPart, assetId).toString();
    }

    /**
     * Return a {@link ContractId} instance parsed from the passed string, that should be in the
     * <code>[definition-id]:[asset-id]:[UUID]</code> format
     *
     * @param id the string representation of the id
     * @return the {@link ContractId} instance that represent the id
     */
    public static Result<ContractId> parseId(String id) {
        if (id == null) {
            return Result.failure("id cannot be null");
        }

        var parts = id.split(":");
        if (parts.length != 3) {
            return Result.failure(format("contract id should be in the form [definition-id]:[asset-id]:[UUID] but it was %s", id));
        }

        var definitionIdPart = parts[DEFINITION_ID_PART];
        var assetIdPart = parts[ASSET_ID_PART];
        var uuidPart = parts[UUID_PART];

        var definitionId = decodeSafely(definitionIdPart);
        var assetId = decodeSafely(assetIdPart);
        var uuid = decodeSafely(uuidPart);

        var contractId = (definitionId == null || assetId == null || uuid == null)
                ? new ContractId(definitionIdPart, assetIdPart, uuidPart)
                : new ContractId(definitionId, assetId, uuid);

        return Result.success(contractId);
    }

    /**
     * Return a {@link ContractId} instance parsed from the passed string, that should be in the
     * <code>[definition-id]:UUID</code> format
     *
     * @param id the string representation of the id
     * @return the {@link ContractId} instance that represent the id
     * @deprecated please use {@link #parseId(String)}
     */
    @Deprecated(since = "0.1.2", forRemoval = true)
    public static ContractId parse(String id) {
        return parseId(id).getContent();
    }

    private static String decodeSafely(String base64string) {
        try {
            return new String(Base64.getDecoder().decode(base64string));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * The id is valid if it follows the following scheme: [definition-id]:UUID
     *
     * @return true if it is valid, false otherwise
     * @deprecated an instantiated {@link ContractId} object is always valid
     */
    @Deprecated(since = "0.1.2", forRemoval = true)
    public boolean isValid() {
        return true;
    }

    /**
     * The definition part of the id
     *
     * @return The definition part of the id
     */
    public String definitionPart() {
        return definitionId;
    }

    /**
     * The asset-id part of the id
     *
     * @return The definition part of the id
     */
    public String assetIdPart() {
        return assetId;
    }

    @Override
    public String toString() {
        var encoder = Base64.getEncoder();
        return encoder.encodeToString(definitionId.getBytes()) +
                DELIMITER +
                encoder.encodeToString(assetId.getBytes()) +
                DELIMITER +
                encoder.encodeToString(uuid.getBytes());
    }

    /**
     * Create a new {@link ContractId} with the same definitionId and assetId but a new random UUID part.
     *
     * @return new {@link ContractId} instance.
     */
    public ContractId derive() {
        return create(definitionId, assetId);
    }
}
