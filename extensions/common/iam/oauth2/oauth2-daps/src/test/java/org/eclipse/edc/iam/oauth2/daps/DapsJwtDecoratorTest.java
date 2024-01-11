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

package org.eclipse.edc.iam.oauth2.daps;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DapsJwtDecoratorTest {

    private final DapsJwtDecorator decorator = new DapsJwtDecorator();

    @Test
    void verifyDecorate() {

        var builder = TokenParameters.Builder.newInstance();
        decorator.decorate(builder);

        var tokenParams = builder.build();
        assertThat(tokenParams.getHeaders()).isEmpty();
        assertThat(tokenParams.getClaims())
                .hasFieldOrPropertyWithValue("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                .hasFieldOrPropertyWithValue("@type", "ids:DatRequestToken");
    }

}