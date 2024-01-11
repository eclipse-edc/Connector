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

package org.eclipse.edc.iam.identitytrust.sts.embedded;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUED_AT;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class SelfIssuedTokenDecoratorTest {

    @Test
    void verifyDecorator() {

        var decorator = new SelfIssuedTokenDecorator(Map.of("iss", "test"), Clock.systemUTC(), 5 * 60);
        var builder = TokenParameters.Builder.newInstance();
        decorator.decorate(builder);
        var tokenParams = builder.build();
        assertThat(tokenParams.getClaims())
                .containsEntry("iss", "test")
                .containsKeys(ISSUED_AT, EXPIRATION_TIME, JWT_ID);

        assertThat(tokenParams.getHeaders()).isEmpty();
    }
}
