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

import org.eclipse.dataspaceconnector.spi.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.schema.Schema;
import org.eclipse.dataspaceconnector.spi.types.domain.schema.SchemaAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaRegistryTest {

    private SchemaRegistry schemaRegistry;

    @BeforeEach
    void setup() {
        schemaRegistry = new SchemaRegistryImpl();
    }

    @Test
    void getSchema_notExist_shouldBeNull() {
        assertThat(schemaRegistry.getSchema("notexist")).isNull();
    }

    @Test
    void getSchema_alreadyExists() {
        final String ident = "test-schema";
        Schema schema = createSchema(ident);
        schemaRegistry.register(schema);
        assertThat(schemaRegistry.getSchema(ident)).isEqualTo(schema);
    }


    @Test
    void registerSchema() {
        var id = "testschema";
        schemaRegistry.register(createSchema(id));
        assertThat(schemaRegistry.hasSchema(id)).isTrue();
    }

    @Test
    void registerSchema_alreadyExists_shouldReplace() {
        var id = "testschema";
        schemaRegistry.register(createSchema(id));

        var newSchema = createSchema(id);
        schemaRegistry.register(newSchema);
        assertThat(schemaRegistry.getSchemas()).containsOnly(newSchema);
    }

    @Test
    void hasSchema_exists() {
        var id = "testschema";
        Schema schema = createSchema(id);
        schemaRegistry.register(schema);

        assertThat(schemaRegistry.hasSchema(id)).isTrue();
    }

    @Test
    void hasSchema_notExist() {
        var id = "testschema";
        Schema schema = createSchema(id);
        schemaRegistry.register(schema);

        assertThat(schemaRegistry.hasSchema("foobar")).isFalse();
    }

    private Schema createSchema(String ident) {
        return new Schema() {
            @Override
            public String getName() {
                return ident;
            }

            @Override
            protected void addAttributes() {
                attributes.add(new SchemaAttribute("some-attr", true));
            }
        };
    }
}
