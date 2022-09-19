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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

class DefaultJwtDecoratorTest {

    private static final long TOKEN_EXPIRATION = 500;

    private String audience;
    private String clientId;
    private final Instant now = Instant.now();

    private DefaultJwtDecorator decorator;

    @BeforeEach
    void setUp() {
        audience = "test-audience";
        clientId = UUID.randomUUID().toString();
        byte[] certificate = new byte[50];
        new Random().nextBytes(certificate);
        var clock = Clock.fixed(now, UTC);
        decorator = new DefaultJwtDecorator(audience, clientId, certificate, clock, TOKEN_EXPIRATION);
    }

    @Test
    void claims() {
        var claims = decorator.claims();

        assertThat(claims)
                .hasEntrySatisfying("aud", o -> assertThat(o).asInstanceOf(list(String.class)).contains(audience))
                .hasFieldOrPropertyWithValue("iss", clientId)
                .hasFieldOrPropertyWithValue("sub", clientId)
                .containsKey("jti")
                .hasEntrySatisfying("iat", issueDate -> assertThat((Date) issueDate).isEqualTo(now))
                .hasEntrySatisfying("exp", expiration -> assertThat((Date) expiration).isEqualTo(now.plusSeconds(TOKEN_EXPIRATION)));
    }

    @Test
    void headers() {
        var result = decorator.headers();

        assertThat(result).containsKey("x5t");
    }

}