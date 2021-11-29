/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.contract.definition.store.model;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.contract.definition.store.TestFunctions.generateNegotiation;

class ContractNegotiationDocumentSerializationTest {

    private TypeManager typeManager;


    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(ContractNegotiationDocument.class, ContractNegotiationDocument.class);
    }

    @Test
    void testSerialization() {
        var def = generateNegotiation();
        var pk = def.getState();

        var document = new ContractNegotiationDocument(def);

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("wrappedInstance")
                .contains("\"id\":\"" + def.getId() + "\"")
                .contains("\"partitionKey\":\"" + pk + "\"");
    }

    @Test
    void testDeserialization() {
        var def = generateNegotiation();

        var document = new ContractNegotiationDocument(def);
        String json = typeManager.writeValueAsString(document);

        var transferProcessDeserialized = typeManager.readValue(json, ContractNegotiationDocument.class);
        assertThat(transferProcessDeserialized).usingRecursiveComparison().isEqualTo(document);
    }
}
