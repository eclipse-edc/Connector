/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.catalog.spi;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSerializationTest {

    private final TypeManager typeManager = new JacksonTypeManager();

    @Test
    void verifySerialization() {
        var catalog = createCatalog();

        var json = typeManager.writeValueAsString(catalog);

        var deserialized = typeManager.readValue(json, Catalog.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(catalog);
    }


    @SuppressWarnings("unchecked")
    private Catalog createCatalog() {
        var dataService = DataService.Builder.newInstance().endpointUrl("http://endpoint").build();

        var distribution = Distribution.Builder.newInstance().dataService(dataService).format("test-format").build();
        var dataset = Dataset.Builder.newInstance()
                .id("assetId")
                .properties(Map.of("key", "val"))
                .offer("offer", Policy.Builder.newInstance().assignee("assignee").assigner("assigner").build())
                .distributions(List.of(distribution))
                .build();

        return Catalog.Builder.newInstance()
                .id("id")
                .dataServices(List.of(dataService))
                .datasets(List.of(dataset))
                .property("prop", "propValue")
                .build();
    }

}
