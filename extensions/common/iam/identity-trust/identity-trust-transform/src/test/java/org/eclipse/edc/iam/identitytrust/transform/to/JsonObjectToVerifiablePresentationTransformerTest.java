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

package org.eclipse.edc.iam.identitytrust.transform.to;

import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.mock;

class JsonObjectToVerifiablePresentationTransformerTest {
    private final TransformerContext context = mock();
    private JsonObjectToVerifiableCredentialTransformer transformer;

    @BeforeEach
    void setup() {
        transformer = new JsonObjectToVerifiableCredentialTransformer(JacksonJsonLd.createObjectMapper());
    }
}