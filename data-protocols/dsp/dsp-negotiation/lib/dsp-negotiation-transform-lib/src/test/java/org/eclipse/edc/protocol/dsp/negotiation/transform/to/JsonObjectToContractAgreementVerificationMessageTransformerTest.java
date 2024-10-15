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

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.to.TestInput.getExpanded;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_IRI;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToContractAgreementVerificationMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectToContractAgreementVerificationMessageTransformer transformer =
            new JsonObjectToContractAgreementVerificationMessageTransformer();

    @BeforeEach
    void setUp() {
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void transform() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "messageId")
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_IRI)
                .add(DSPACE_PROPERTY_CONSUMER_PID_IRI, "consumerPid")
                .add(DSPACE_PROPERTY_PROVIDER_PID_IRI, "providerPid")
                .build();

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractAgreementVerificationMessage.class);
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getConsumerPid()).isEqualTo("consumerPid");
        assertThat(result.getProviderPid()).isEqualTo("providerPid");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_failTransformWhenConsumerPidMissing() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "messageId")
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_IRI)
                .build();

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNull();

        verify(context, times(1)).reportProblem(contains(DSPACE_PROPERTY_CONSUMER_PID_IRI));
    }
}
