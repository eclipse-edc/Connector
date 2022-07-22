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

package org.eclipse.dataspaceconnector.spi.types;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TypeManagerTest {

    @Test
    void verifySerialization() throws JsonProcessingException {
        var manager = new TypeManager();
        manager.registerSerializer("foo", Bar.class, new JsonSerializer<>() {
            @Override
            public void serialize(Bar value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
                generator.writeString(value.toString());
            }
        });

        var fooMapper = manager.getMapper("foo");
        assertThat(fooMapper).isNotNull();

        var result = fooMapper.writeValueAsString(new Bar());
        assertThat(result).isNotNull();
    }

    @Test
    void decorateExample() throws JsonProcessingException {
        var manager = new TypeManager();
        var fooMapper = manager.getMapper("foo");

        manager.registerSerializer("foo", Bar.class, new DecoratingSerializer<>(Bar.class));
        manager.registerSerializer("foo", Baz.class, new DecoratingSerializer<>(Baz.class));

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

    private static class DecoratingSerializer<T> extends JsonSerializer<T> {
        private final Class<T> type;

        DecoratingSerializer(Class<T> type) {

            this.type = type;
        }

        public void serialize(Object value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeStartObject();
            var javaType = provider.constructType(type);
            var beanDescription = provider.getConfig().introspect(javaType);
            var staticTyping = provider.isEnabled(MapperFeature.USE_STATIC_TYPING);
            var serializer = BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDescription, staticTyping);
            serializer.unwrappingSerializer(null).serialize(value, generator, provider);
            generator.writeObjectField("@context", "some data");
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
