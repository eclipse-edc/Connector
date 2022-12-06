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

package org.eclipse.edc.connector.transfer.spi.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResourceDefinitionTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TestResourceDefinition process = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        TestResourceDefinition deserialized = mapper.readValue(writer.toString(), TestResourceDefinition.class);

        assertNotNull(deserialized);
    }


}
