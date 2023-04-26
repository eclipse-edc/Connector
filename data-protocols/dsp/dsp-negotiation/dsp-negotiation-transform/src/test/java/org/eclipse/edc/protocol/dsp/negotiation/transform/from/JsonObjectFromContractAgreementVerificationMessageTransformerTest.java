/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.CRED_CREDENTIAL_SUBJECT;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.SEC_PROOF;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromContractAgreementVerificationMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromContractAgreementVerificationMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromContractAgreementVerificationMessageTransformer(jsonFactory);
    }

    @Test
    void transform() {
        var value = "example";
        var message = ContractAgreementVerificationMessage.Builder.newInstance()
                .protocol(value)
                .processId(value)
                .callbackAddress(value)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isNotEmpty();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo(value);
        assertThat(result.getJsonString(CRED_CREDENTIAL_SUBJECT)).isNull();
        assertThat(result.getJsonString(SEC_PROOF)).isNull();

        verify(context, never()).reportProblem(anyString());
    }
}