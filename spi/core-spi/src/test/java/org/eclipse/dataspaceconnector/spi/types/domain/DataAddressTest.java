/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.types.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataAddressTest {

    private static final Faker FAKER = new Faker();
    private static final String TYPE_NAME = FAKER.lorem().word();
    private static final String KEY_NAME = FAKER.lorem().word();
    private static final String PROPERTY_KEY = FAKER.lorem().word();
    private static final String PROPERTY_VALUE_ORIGINAL = FAKER.lorem().word();
    private static final String PROPERTY_VALUE_CHANGED = FAKER.lorem().word();


    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type("test")
                .keyName("somekey")
                .property("foo", "bar").build();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, dataAddress);

        DataAddress deserialized = mapper.readValue(writer.toString(), DataAddress.class);

        assertThat(deserialized).isNotNull();

        assertThat(deserialized.getType()).isEqualTo("test");
        assertThat(deserialized.getProperty("foo")).isEqualTo("bar");
    }

    @Test
    void verifyNoTypeThrowsException() {
        assertThatThrownBy(() -> DataAddress.Builder.newInstance().keyName("somekey").property("foo", "bar").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DataAddress builder missing Type property.");
    }

    @Test
    void verifyNullKeyThrowsException() {
        assertThatThrownBy(() -> DataAddress.Builder.newInstance().type("sometype").keyName("somekey").property(null, "bar").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Property key null.");
    }

    @Test
    void verifyGetDefaultPropertyValue() {
        assertThat(DataAddress.Builder.newInstance().type("sometype").build().getProperty("missing", "defaultValue"))
                .isEqualTo("defaultValue");
    }

    @Test
    void verifyGetExistingPropertyValue() {
        assertThat(DataAddress.Builder.newInstance().type("sometype").property("existing", "existingValue").build().getProperty("existing", "defaultValue"))
                .isEqualTo("existingValue");
    }

    @Test
    void verifyCopy() {
        DataAddress dataAddress = newSampleDataAddress();

        var copy = dataAddress.copy();

        assertThat(copy).usingRecursiveComparison().isEqualTo(dataAddress);
    }

    @Test
    void verifyDeepCopy() {
        DataAddress dataAddress = newSampleDataAddress();

        var copy = dataAddress.copy();

        var copyProperties = copy.getProperties();
        copyProperties.put(PROPERTY_KEY, PROPERTY_VALUE_CHANGED);

        assertThat(dataAddress.getProperty(PROPERTY_KEY)).isEqualTo(PROPERTY_VALUE_ORIGINAL);
    }

    @Test
    void verifyToBuilder() {
        DataAddress dataAddress = newSampleDataAddress();

        var copy = dataAddress.toBuilder().build();

        assertThat(copy).usingRecursiveComparison().isEqualTo(dataAddress);
    }

    private DataAddress newSampleDataAddress() {
        return DataAddress.Builder
                .newInstance()
                .type(TYPE_NAME)
                .keyName(KEY_NAME)
                .property(PROPERTY_KEY, PROPERTY_VALUE_ORIGINAL)
                .build();
    }
}
