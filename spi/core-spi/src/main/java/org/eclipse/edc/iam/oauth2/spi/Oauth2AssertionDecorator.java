/*
 *  Copyright (c) 2020 - 2022 Amadeus
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

package org.eclipse.edc.iam.oauth2.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;


public class Oauth2AssertionDecorator implements TokenDecorator {

    private String audience;
    private String clientId;
    private Clock clock;
    private long validity;
    private String kid;

    private Oauth2AssertionDecorator() {
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        new KeyIdDecorator(kid).decorate(tokenParameters);
        return tokenParameters.claims(AUDIENCE, List.of(audience))
                .claims(ISSUER, clientId)
                .claims(SUBJECT, clientId)
                .claims(JWT_ID, UUID.randomUUID().toString())
                .claims(ISSUED_AT, clock.instant().getEpochSecond())
                .claims(EXPIRATION_TIME, clock.instant().plusSeconds(validity).getEpochSecond());
    }

    public static class Builder {

        private final Oauth2AssertionDecorator decorator;

        private Builder() {
            decorator = new Oauth2AssertionDecorator();
        }

        @JsonCreator
        public static Oauth2AssertionDecorator.Builder newInstance() {
            return new Oauth2AssertionDecorator.Builder();
        }

        public Builder audience(String audience) {
            decorator.audience = audience;
            return this;
        }

        public Builder clientId(String clientId) {
            decorator.clientId = clientId;
            return this;
        }

        public Builder clock(Clock clock) {
            decorator.clock = clock;
            return this;
        }

        public Builder validity(long validity) {
            decorator.validity = validity;
            return this;
        }

        public Builder kid(String kid) {
            decorator.kid = kid;
            return this;
        }

        public Oauth2AssertionDecorator build() {
            Objects.requireNonNull(decorator.audience, "Audience must be set");
            Objects.requireNonNull(decorator.clientId, "Client ID must be set");
            if (decorator.validity <= 0) {
                throw new IllegalArgumentException("Validity must be greater than 0");
            }
            decorator.clock = Objects.requireNonNullElse(decorator.clock, Clock.systemUTC());
            return decorator;
        }
    }
}
