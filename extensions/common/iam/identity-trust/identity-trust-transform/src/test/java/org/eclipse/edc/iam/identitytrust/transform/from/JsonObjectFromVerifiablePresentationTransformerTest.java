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

package org.eclipse.edc.iam.identitytrust.transform.from;

import org.eclipse.edc.iam.identitytrust.transform.TestConstants;
import org.eclipse.edc.identitytrust.TestFunctions;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromVerifiablePresentationTransformerTest {

    private final TransformerContext context = mock();
    private JsonObjectFromVerifiablePresentationTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromVerifiablePresentationTransformer(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var vp = TestFunctions.createPresentationBuilder(TestConstants.EXAMPLE_VP_JSONLD, CredentialFormat.JSON_LD).build();
        var jsonObj = transformer.transform(vp, context);

        verify(context, never()).reportProblem(anyString());
        assertThat(jsonObj).isNotNull();
        assertThat(jsonObj.getString(VerifiablePresentation.VERIFIABLE_PRESENTATION_ID_PROPERTY)).isEqualTo("test-id");
        assertThat(jsonObj.getString(VerifiablePresentation.VERIFIABLE_PRESENTATION_TYPE_PROPERTY)).isEqualTo("VerifiablePresentation");
        assertThat(jsonObj.getString(VerifiablePresentation.VERIFIABLE_PRESENTATION_HOLDER_PROPERTY)).isEqualTo("did:web:test-holder");
        assertThat(jsonObj.getJsonArray(VerifiablePresentation.VERIFIABLE_PRESENTATION_VC_PROPERTY)).hasSize(1);
        assertThat(jsonObj.getJsonObject(VerifiablePresentation.VERIFIABLE_PRESENTATION_PROOF_PROPERTY).entrySet()).hasSize(5);
    }
}