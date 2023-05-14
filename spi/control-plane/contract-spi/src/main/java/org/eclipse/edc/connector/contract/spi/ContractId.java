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

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles contract ID generation for contract offers and agreements originating in an EDC runtime.
 * Ids are architected to allow the contract definition which generated the contract to be de-referenced.
 * The id format follows the following scheme: <code>[definition-id]:[asset-id]:UUID</code>
 */
public final class ContractId {

    private static final int DEFINITION_PART = 0;
    private static final int ASSET_ID_PART = 1;
    private static final String DELIMITER = ":";
    private final String value;

    private ContractId(String value) {
        this.value = value;
    }

    /**
     * Returns a new id given the definition part
     *
     * @param definitionPart the part that will be used as prefix of the id
     * @param assetId        The ID of the asset that is contained in the offer
     * @return a {@link String} that represent the contract id
     */
    @NotNull
    public static String createContractId(String definitionPart, String assetId) {
        return definitionPart + DELIMITER + assetId + DELIMITER + UUID.randomUUID();
    }

    /**
     * Return a {@link ContractId} instance parsed from the passed string, that should be in the
     * <code>[definition-id]:UUID</code> format
     *
     * @param id the string representation of the id
     * @return the {@link ContractId} instance that represent the id
     */
    public static ContractId parse(String id) {
        return new ContractId(id);
    }

    /**
     * The id is valid if it follows the following scheme: [definition-id]:UUID
     *
     * @return true if it is valid, false otherwise
     */
    public boolean isValid() {
        var parts = parseContractId(value);
        return parts.length == 3;
    }

    /**
     * The definition part of the id
     *
     * @return The definition part of the id
     */
    public String definitionPart() {
        var parts = parseContractId(value);
        return parts[DEFINITION_PART];
    }

    /**
     * The asset-id part of the id
     *
     * @return The definition part of the id
     */
    public String assetIdPart() {
        var parts = parseContractId(value);
        return parts[ASSET_ID_PART];
    }

    private String[] parseContractId(@NotNull String id) {
        return id.split(DELIMITER);
    }

    @Override
    public String toString() {
        return value;
    }
}
