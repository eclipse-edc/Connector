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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DapsJwtDecoratorTest {

    private final DapsJwtDecorator decorator = new DapsJwtDecorator();

    @Test
    void claims() {
        var result = decorator.claims();

        assertThat(result)
                .hasFieldOrPropertyWithValue("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                .hasFieldOrPropertyWithValue("@type", "ids:DatRequestToken");
    }

    @Test
    void headers() {
        var result = decorator.headers();

        assertThat(result).isNotNull().isEmpty();
    }
}