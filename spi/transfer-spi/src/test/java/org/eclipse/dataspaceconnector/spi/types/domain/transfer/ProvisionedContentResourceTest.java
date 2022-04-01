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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProvisionedContentResourceTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        var resource = TestProvisionedContentResource.Builder.newInstance()
                .resourceName("test")
                .dataAddress(dataAddress)
                .id("1")
                .transferProcessId("2")
                .resourceDefinitionId("12")
                .build();
        var mapper = new ObjectMapper();
        var serialized = mapper.writeValueAsString(resource);
        var deserialized = mapper.readValue(serialized, TestProvisionedContentResource.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getDataAddress()).isNotNull();
        assertThat(deserialized.getResourceName()).isEqualTo("test");
        assertThat(deserialized.getId()).isEqualTo("1");
        assertThat(deserialized.getTransferProcessId()).isEqualTo("2");
        assertThat(deserialized.getResourceDefinitionId()).isEqualTo("12");
    }

    @JsonTypeName("dataspaceconnector:testprovisioneddcontentresource")
    @JsonDeserialize(builder = TestProvisionedContentResource.Builder.class)
    private static class TestProvisionedContentResource extends ProvisionedContentResource {

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ProvisionedContentResource.Builder<TestProvisionedContentResource, Builder> {

            protected Builder() {
                super(new TestProvisionedContentResource());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        }
    }
}
