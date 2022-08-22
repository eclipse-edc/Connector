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

package org.eclipse.dataspaceconnector.iam.daps;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DapsJwtDecoratorTest {

    private DapsJwtDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new DapsJwtDecorator();
    }

    @Test
    void decorate() {
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256);
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();

        decorator.decorate(headerBuilder, claimsBuilder);

        JWSHeader header = headerBuilder.build();
        JWTClaimsSet claims = claimsBuilder.build();

        assertThat(header.getIncludedParams()).hasSize(1);
        assertThat(claims.getClaims())
                .hasFieldOrPropertyWithValue("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                .hasFieldOrPropertyWithValue("@type", "ids:DatRequestToken");
    }
}