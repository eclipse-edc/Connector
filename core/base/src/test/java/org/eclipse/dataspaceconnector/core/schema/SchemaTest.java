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

package org.eclipse.dataspaceconnector.core.schema;

import org.eclipse.dataspaceconnector.spi.types.domain.schema.Schema;
import org.eclipse.dataspaceconnector.spi.types.domain.schema.SchemaAttribute;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaTest {

    @Test
    void getAttributes() {
        Schema schema = createSchema();
        assertThat(schema.getAttributes()).hasSize(2);
    }

    @Test
    void getAttributes_addWithSameName() {
        var schema = createSchema();
        var hasAdded = schema.getAttributes().add(new SchemaAttribute("test", false));
        assertThat(hasAdded).isTrue();
    }

    @Test
    void getRequiredAttributes() {
        var schema = createSchema();
        schema.getAttributes().add(new SchemaAttribute("foo", true));
        schema.getAttributes().add(new SchemaAttribute("bar", false));

        assertThat(schema.getRequiredAttributes()).hasSize(3)
                .anyMatch(sa -> sa.getName().equals("foo"))
                .anyMatch(sa -> sa.getName().equals("test"));
    }

    @NotNull
    private Schema createSchema() {
        return new Schema() {
            @Override
            public String getName() {
                return "testSchema";
            }

            @Override
            protected void addAttributes() {
                attributes.add(new SchemaAttribute("test", true));
            }
        };
    }

}
