/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.ConnectMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConnectMessageTest {

    @Test
    void verifyDeserialization() throws IOException {
        var mapper = new ObjectMapper();

        var dataMessage = ConnectMessage.Builder.newInstance();
        var writer = new StringWriter();
        mapper.writeValue(writer, dataMessage.build());

        var deserialized = mapper.readValue(writer.toString(), ConnectMessage.class);

        assertNotNull(deserialized);
    }

}
