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

package org.eclipse.edc.connector.controlplane.contract.spi;

import org.eclipse.edc.spi.result.Result;

import java.util.Base64;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Handles contract ID generation for contract offers originating in an EDC runtime.
 * Ids are architected to allow the contract definition which generated the contract to be de-referenced.
 * The id format follows the following scheme: <code>[definition-id]:[asset-id]:[UUID]</code>
 */
public final class ContractOfferId {

    private static final String DELIMITER = ":";
    private static final int DEFINITION_ID_PART = 0;
    private static final int ASSET_ID_PART = 1;
    private static final int UUID_PART = 2;
    private final String definitionId;
    private final String assetId;
    private final String uuid;

    private ContractOfferId(String definitionId, String assetId, String uuid) {
        this.definitionId = definitionId;
        this.assetId = assetId;
        this.uuid = uuid;
    }

    public static ContractOfferId create(String definitionId, String assetId) {
        return new ContractOfferId(definitionId, assetId, UUID.randomUUID().toString());
    }

    /**
     * Return a {@link ContractOfferId} instance parsed from the passed string, that should be in the
     * <code>[definition-id]:[asset-id]:[UUID]</code> format
     *
     * @param id the string representation of the id
     * @return the {@link ContractOfferId} instance that represent the id
     */
    public static Result<ContractOfferId> parseId(String id) {
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
                ? new ContractOfferId(definitionIdPart, assetIdPart, uuidPart)
                : new ContractOfferId(definitionId, assetId, uuid);

        return Result.success(contractId);
    }

    private static String decodeSafely(String base64string) {
        try {
            return new String(Base64.getDecoder().decode(base64string));
        } catch (IllegalArgumentException exception) {
            return null;
        }
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

}
