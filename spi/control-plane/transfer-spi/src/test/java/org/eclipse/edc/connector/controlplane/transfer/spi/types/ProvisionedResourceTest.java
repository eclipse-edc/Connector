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

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProvisionedResourceTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TestProvisionedResource process = TestProvisionedResource.Builder.newInstance().id(UUID.randomUUID().toString()).transferProcessId("123").resourceDefinitionId("1").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        TestProvisionedResource deserialized = mapper.readValue(writer.toString(), TestProvisionedResource.class);

        assertNotNull(deserialized);
    }


}
