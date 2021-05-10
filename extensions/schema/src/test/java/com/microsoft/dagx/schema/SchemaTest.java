/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema;

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
            protected void addAttributes() {
                attributes.add(new SchemaAttribute("test", true));
            }

            @Override
            public String getName() {
                return "testSchema";
            }
        };
    }

}
