/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.json;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonTypeManagerTest {

    private final JacksonTypeManager typeManager = new JacksonTypeManager();

    @Test
    void verifySerialization() {
        typeManager.registerSerializer("foo", Bar.class, new ValueSerializer<>() {
            @Override
            public void serialize(Bar value, JsonGenerator generator, SerializationContext serializers) {
                generator.writeString(value.toString());
            }
        });

        var fooMapper = typeManager.getMapper("foo");
        assertThat(fooMapper).isNotNull();

        var result = fooMapper.writeValueAsString(new Bar());
        assertThat(result).isNotNull();
    }

    @Test
    void decorateExample() {
        var fooMapper = typeManager.getMapper("foo");

        typeManager.registerSerializer("foo", Bar.class, new DecoratingSerializer<>());
        typeManager.registerSerializer("foo", Baz.class, new DecoratingSerializer<>());

        var baz = new Baz();
        baz.setName("name");

        var bar = new Bar();
        bar.setId("test");
        bar.setBaz(baz);

        var result = fooMapper.writeValueAsString(bar);

        assertThat(result).isNotNull();

        var obj = fooMapper.readValue(result, Bar.class);
        assertThat(obj).isInstanceOf(Bar.class);
        assertThat(obj.getBaz()).isInstanceOf(Baz.class);
    }

    /**
     * Serializes the bean's regular properties and decorates the output with an additional {@code @context} field.
     * A plain, non-decorated mapper is used to render the bean's own properties, which avoids recursing back into
     * this (registered) serializer.
     */
    private static class DecoratingSerializer<T> extends ValueSerializer<T> {
        private static final ObjectMapper PLAIN_MAPPER = JsonMapper.builder().build();

        @Override
        public void serialize(T value, JsonGenerator generator, SerializationContext context) {
            generator.writeStartObject();
            var node = PLAIN_MAPPER.valueToTree(value);
            node.properties().forEach(entry -> generator.writePOJOProperty(entry.getKey(), entry.getValue()));
            generator.writeStringProperty("@context", "some data");
            generator.writeEndObject();
        }
    }

    private static class Bar {
        private String id;
        private Baz baz;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Baz getBaz() {
            return baz;
        }

        public void setBaz(Baz baz) {
            this.baz = baz;
        }
    }

    private static class Baz {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
