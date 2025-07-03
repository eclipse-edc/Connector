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

package org.eclipse.edc.spi.types.domain;

import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;

class DataAddressTest {

    @Test
    void verifyDeserialization() throws IOException {
        var mapper = new JacksonTypeManager().getMapper();
        Map<String, Object> responseChannelMap = Map.of(
                "url", "http://example.com/response",
                "type", "someothertype"
        );

        var dataAddress = DataAddress.Builder.newInstance()
                .type("test")
                .keyName("somekey")
                .responseChannel(responseChannelMap)
                .property("foo", "bar").build();
        var writer = new StringWriter();
        mapper.writeValue(writer, dataAddress);

        var deserialized = mapper.readValue(writer.toString(), DataAddress.class);

        assertThat(deserialized).isNotNull();

        assertThat(deserialized.getType()).isEqualTo("test");
        assertThat(deserialized.getStringProperty("foo")).isEqualTo("bar");
        assertThat(deserialized.getResponseChannel())
                .isInstanceOf(DataAddress.class)
                .usingRecursiveComparison()
                .isEqualTo(DataAddress.Builder.newInstance().properties(responseChannelMap).build());
    }

    @Test
    void verifyDeserializationWithComplexAttribute() throws IOException {

        var mapper = new JacksonTypeManager().getMapper();
        var dataAddress = DataAddress.Builder.newInstance()
                .type("test")
                .keyName("somekey")
                .property("foo", "bar")
                .property("complexJsonObject", DataAddress.Builder.newInstance().property(EDC_DATA_ADDRESS_TYPE_PROPERTY, "AmazonS3").build())
                .property("complexJsonArray", List.of("string1", "string2"))
                .build();
        var writer = new StringWriter();
        mapper.writeValue(writer, dataAddress);
        var deserialized = mapper.readValue(writer.toString(), DataAddress.class);
        var jsonObject = mapper.convertValue(deserialized.getProperty("complexJsonObject"), DataAddress.class);
        var jsonArray = mapper.convertValue(deserialized.getProperty("complexJsonArray"), List.class);
        assertThat(deserialized)
                .isNotNull();
        assertThat(jsonObject)
                .isNotNull()
                .usingRecursiveComparison().isEqualTo(dataAddress.getProperty("complexJsonObject"));
        assertThat(jsonArray)
                .isNotNull()
                .usingRecursiveComparison().isEqualTo(dataAddress.getProperty("complexJsonArray"));
    }

    @Test
    void verifyNoTypeThrowsException() {
        assertThatNullPointerException().isThrownBy(() -> DataAddress.Builder.newInstance()
                        .keyName("somekey")
                        .property("foo", "bar")
                        .build())
                .withMessageContaining("DataAddress builder missing Type property.");
    }

    @Test
    void verifyNullKeyThrowsException() {
        assertThatThrownBy(() -> DataAddress.Builder.newInstance().type("sometype").keyName("somekey").property(null, "bar").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Property key null.");


        assertThatNullPointerException().isThrownBy(() -> DataAddress.Builder.newInstance()
                        .type("sometype")
                        .keyName("somekey")
                        .property(null, "bar")
                        .build())
                .withMessageContaining("Property key null.");
    }

    @Test
    void verifyGetDefaultPropertyValue() {
        assertThat(DataAddress.Builder.newInstance().type("sometype").build().getStringProperty("missing", "defaultValue"))
                .isEqualTo("defaultValue");
    }

    @Test
    void verifyGetExistingPropertyValue() {
        var address = DataAddress.Builder.newInstance()
                .type("sometype")
                .property("existing", "aValue")
                .property(EDC_NAMESPACE + "anotherExisting", "anotherValue")
                .build();

        assertThat(address.getStringProperty("existing", "defaultValue")).isEqualTo("aValue");
        assertThat(address.getStringProperty("anotherExisting", "defaultValue")).isEqualTo("anotherValue");
    }

    @Test
    void verifyHasProperty() {
        var address = DataAddress.Builder.newInstance()
                .type("sometype")
                .property("existing", "aValue")
                .property(EDC_NAMESPACE + "anotherExisting", "anotherValue")
                .build();

        assertThat(address.hasProperty("existing")).isTrue();
        assertThat(address.hasProperty("anotherExisting")).isTrue();
        assertThat(address.hasProperty("unknown")).isFalse();
    }

    @Test
    void verifyResponseChannelBuilderFromDataAddress() {
        var responseChannelDataAddress = DataAddress.Builder.newInstance()
                .type("someothertype")
                .property("url", "http://example.com/response")
                .build();
        var address = DataAddress.Builder.newInstance()
                .type("sometype")
                .responseChannel(responseChannelDataAddress)
                .build();

        assertThat(address.getResponseChannel())
                .isNotNull()
                .isInstanceOf(DataAddress.class)
                .isEqualTo(responseChannelDataAddress);
    }

    @Test
    void verifyResponseChannelBuilderFromMap() {

        Map<String, Object> responseChannelMap = Map.of(
                "url", "http://example.com/response",
                "type", "someothertype"
        );

        var address = DataAddress.Builder.newInstance()
                .type("sometype")
                .responseChannel(responseChannelMap)
                .build();

        assertThat(address.getResponseChannel())
                .isNotNull()
                .isInstanceOf(DataAddress.class)
                .usingRecursiveComparison()
                .isEqualTo(DataAddress.Builder.newInstance().properties(responseChannelMap).build());
    }


}
