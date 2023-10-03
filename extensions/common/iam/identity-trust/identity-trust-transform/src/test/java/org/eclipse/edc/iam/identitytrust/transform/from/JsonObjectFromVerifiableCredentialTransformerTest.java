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

import org.eclipse.edc.identitytrust.TestFunctions;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.transform.TestConstants.EXAMPLE_VC_JSONLD;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_DESCRIPTION_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_ID_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_ISSUER_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_NAME_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_STATUS_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_SUBJECT_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_TYPE_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_VALIDFROM_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_VALIDUNTIL_PROPERTY;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JsonObjectFromVerifiableCredentialTransformerTest {

    private final TransformerContext context = mock();
    private JsonObjectFromVerifiableCredentialTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromVerifiableCredentialTransformer(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var vc = TestFunctions.createCredentialBuilder(EXAMPLE_VC_JSONLD, CredentialFormat.JSON_LD)
                .build();

        var json = transformer.transform(vc, context);
        assertThat(json).isNotNull();
        assertThat(json.getString(VERIFIABLE_CREDENTIAL_ID_PROPERTY)).isEqualTo("http://university.example/credentials/3732");
        assertThat(json.getJsonArray(VERIFIABLE_CREDENTIAL_TYPE_PROPERTY)).hasSize(2);
        assertThat(json.getJsonObject(VERIFIABLE_CREDENTIAL_ISSUER_PROPERTY).entrySet()).hasSize(3);
        assertThat(json.getString(VERIFIABLE_CREDENTIAL_VALIDFROM_PROPERTY)).isEqualTo("2015-05-10T12:30:00Z");
        assertThat(json.getString(VERIFIABLE_CREDENTIAL_NAME_PROPERTY)).isEqualTo("Example University Degree");
        assertThat(json.getString(VERIFIABLE_CREDENTIAL_DESCRIPTION_PROPERTY)).isEqualTo("2015 Bachelor of Science and Arts Degree");
        assertThat(json.get(VERIFIABLE_CREDENTIAL_VALIDUNTIL_PROPERTY)).isNull();
        assertThat(json.get(VERIFIABLE_CREDENTIAL_STATUS_PROPERTY)).isNull();
        assertThat(json.getJsonObject(VERIFIABLE_CREDENTIAL_SUBJECT_PROPERTY).entrySet()).hasSize(2);
    }

    @Test
    void transform_wrongFormat() {
        var vc = TestFunctions.createCredentialBuilder("foobar", CredentialFormat.JWT)
                .build();

        assertThat(transformer.transform(vc, context)).isNull();
        verify(context).reportProblem(contains("This VerifiableCredential is expected to be in JSON_LD format but was JWT"));
    }
}