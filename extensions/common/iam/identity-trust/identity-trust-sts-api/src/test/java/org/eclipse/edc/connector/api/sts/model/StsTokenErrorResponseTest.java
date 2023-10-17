/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.sts.model;

import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StsTokenErrorResponseTest {

    @Test
    void verifyDeserialize() throws IOException {
        var mapper = new TypeManager().getMapper();

        var response = new StsTokenErrorResponse("error", "description", "uri");
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, response);

        var deserialized = mapper.readValue(writer.toString(), StsTokenErrorResponse.class);

        assertNotNull(deserialized);
        assertThat(deserialized).isEqualTo(response);
    }
}
