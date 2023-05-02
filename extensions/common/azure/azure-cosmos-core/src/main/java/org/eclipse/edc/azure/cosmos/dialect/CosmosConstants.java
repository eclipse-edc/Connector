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

package org.eclipse.edc.azure.cosmos.dialect;

import java.util.List;

/**
 * Provides some constant values that are relevant in the realm of CosmosDB
 */
public interface CosmosConstants {
    List<String> ILLEGAL_CHARACTERS = List.of("\\", "/", "$", "%", "&");
    String WHERE = "WHERE";
    String AND = "AND";
    String LIMIT = "LIMIT";
    String OFFSET = "OFFSET";
    String ORDER_BY = "ORDER BY";

    static boolean hasIllegalCharacters(String inputStr) {
        return ILLEGAL_CHARACTERS.stream().anyMatch(inputStr::contains);
    }

}
