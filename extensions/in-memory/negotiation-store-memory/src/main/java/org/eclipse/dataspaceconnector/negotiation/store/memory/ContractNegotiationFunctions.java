/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

/**
 * Internal utility functions specifically for use with the {@link InMemoryContractNegotiationStore}.
 */
class ContractNegotiationFunctions {

    /**
     * Utility function to get value of a field from a {@link ContractNegotiation}.
     * @param n The {@link ContractNegotiation} object
     * @param key The name of the field
     * @return The field's value. Returns null if the field does not exist.
     */
    static <T> T property(ContractNegotiation n, String key) {
        try {
            var field = n.getClass().getDeclaredField(key);
            field.setAccessible(true);
            return (T) field.get(n);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            //ignored
        }
        return null;

    }
}
