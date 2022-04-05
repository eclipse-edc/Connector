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

package org.eclipse.dataspaceconnector.contract.common;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles contract ID generation for contract offers and agreements originating in an EDC runtime.
 *
 * Ids are architected to allow the contract definition which generated the contract to be de-referenced. The id format follows the following scheme: [definition-id]:UUID.
 */
public final class ContractId {
    public static final int DEFINITION_PART = 0;
    private static final String DELIMITER = ":";

    /**
     * Returns a UUID that references the definition id.
     */
    @NotNull
    public static String createContractId(String definitionPart) {
        return definitionPart + DELIMITER + UUID.randomUUID();
    }

    /**
     * Parses a contract id into its definition and sub-identifier parts. Use {@link #DEFINITION_PART} to access the definition part.
     */
    public static String[] parseContractId(@NotNull String id) {
        return id.split(DELIMITER);
    }

    private ContractId() {
    }
}
