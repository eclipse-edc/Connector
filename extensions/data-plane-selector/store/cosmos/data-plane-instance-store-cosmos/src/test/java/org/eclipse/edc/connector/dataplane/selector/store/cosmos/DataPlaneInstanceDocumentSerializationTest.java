/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.store.cosmos;

import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.store.TestFunctions.generateDataPlaneInstance;

class DataPlaneInstanceDocumentSerializationTest {

    private TypeManager typeManager;


    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstanceDocument.class, DataPlaneInstanceDocument.class);
    }

    @Test
    void testSerialization() {
        var def = generateDataPlaneInstance();
        var pk = "test-part-key";

        var document = new DataPlaneInstanceDocument(def, pk);

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("properties\":{\"somekey-2\":\"someval-2\",\"somekey-1\":\"someval-1\"}")
                .contains("wrappedInstance")
                .contains("\"id\":\"" + def.getId() + "\"")
                .contains("\"partitionKey\":\"" + pk + "\"");
    }

    @Test
    void testDeserialization() {
        var def = generateDataPlaneInstance();

        var document = new DataPlaneInstanceDocument(def, "test-part-key");
        String json = typeManager.writeValueAsString(document);

        var dataPlaneDeserialized = typeManager.readValue(json, DataPlaneInstanceDocument.class);
        assertThat(dataPlaneDeserialized).usingRecursiveComparison().isEqualTo(document);
    }
}
