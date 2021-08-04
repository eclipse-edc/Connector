/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.object.ObjectStorageMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class ObjectStorageMessageTest {

    @Test
    void verifyDeserialization() throws IOException {
        var mapper = new ObjectMapper();

        var dataMessage = ObjectStorageMessage.Builder.newInstance();
        dataMessage.accessToken("token");
        dataMessage.data("content".getBytes());
        dataMessage.containerName("container");
        dataMessage.key("key");
        var writer = new StringWriter();
        mapper.writeValue(writer, dataMessage.build());

        var deserialized = mapper.readValue(writer.toString(), ObjectStorageMessage.class);

        assertNotNull(deserialized);
    }
}
