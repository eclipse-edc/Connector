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
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StsTokenResponseTest {

    @Test
    void verifyDeserialize() throws IOException {
        var mapper = new TypeManager().getMapper();

        var accessToken = "token";
        var expiration = Clock.systemUTC().millis();
        var tokenResponse = new StsTokenResponse(accessToken, expiration);
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, tokenResponse);

        var deserialized = mapper.readValue(writer.toString(), StsTokenResponse.class);

        assertNotNull(deserialized);
        assertThat(deserialized).isEqualTo(tokenResponse);
        assertThat(deserialized.tokenType()).isEqualTo("Bearer");
    }
}
