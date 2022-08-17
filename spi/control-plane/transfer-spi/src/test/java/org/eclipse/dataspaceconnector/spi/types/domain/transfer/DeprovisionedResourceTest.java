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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class DeprovisionedResourceTest {

    @Test
    void verifyDeserialization() throws IOException {
        var mapper = new ObjectMapper();

        var resource = DeprovisionedResource.Builder.newInstance().provisionedResourceId("123")
                .inProcess(true)
                .error(true)
                .errorMessage("foo")
                .build();
        var writer = new StringWriter();
        mapper.writeValue(writer, resource);

        var deserialized = mapper.readValue(writer.toString(), DeprovisionedResource.class);

        assertThat(deserialized).isNotNull().usingRecursiveComparison().isEqualTo(resource);
        assertThat(deserialized.isInProcess()).isTrue();
        assertThat(deserialized.isError()).isTrue();
        assertThat(deserialized.getErrorMessage()).isEqualTo("foo");

    }
}
