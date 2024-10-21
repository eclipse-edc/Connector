/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.embedded;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.NOT_BEFORE;

class AccessTokenDecoratorTest {

    @Test
    void verifyExpectedClaims() {
        var builder = TokenParameters.Builder.newInstance();
        var now = Instant.now();
        var decorator = new AccessTokenDecorator("test-id", now, now.plusSeconds(5), Map.of("claim1", "value1"));
        decorator.decorate(builder);

        var tokenParams = builder.build();
        assertThat(tokenParams.getClaims())
                .containsEntry("claim1", "value1")
                .containsEntry(JWT_ID, "test-id")
                .containsKeys(ISSUED_AT, EXPIRATION_TIME, NOT_BEFORE);
    }
}