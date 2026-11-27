/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.controlplane.ProcessRemoteMessage;
import org.eclipse.edc.json.JsonMapperFactory;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.test.TestJsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementVerificationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationEventMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractOfferMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferSuspensionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferTerminationMessageTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_AGREEMENT_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_EVENT_TYPE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_TIMESTAMP_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Every inbound DSP message transformer keeps a {@code urn:uuid:} message {@code @id}, and replaces any other one.
 */
class DspMessageIdentifierTransformersTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("https://w3id.org/dspace/2025/1/");
    private static final String MESSAGE_ID = "urn:uuid:3f0a6a31-5c3b-4f52-9a2e-2b1f0c7d9e11";

    private final TransformerContext context = mock();

    @BeforeEach
    void setUp() {
        when(context.problem()).thenReturn(new ProblemBuilder(context));
        when(context.transform(any(JsonObject.class), eq(Policy.class)))
                .thenReturn(Policy.Builder.newInstance().assigner("provider").assignee("consumer").target("assetId").build());
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(Transformers.class)
    void shouldKeepUrnUuidMessageId(String type, TypeTransformer<JsonObject, ? extends ProcessRemoteMessage> transformer, Consumer<JsonObjectBuilder> body) {
        var result = transformer.transform(message(type, MESSAGE_ID, body), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(MESSAGE_ID);
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(Transformers.class)
    void shouldReplaceOtherMessageId(String type, TypeTransformer<JsonObject, ? extends ProcessRemoteMessage> transformer, Consumer<JsonObjectBuilder> body) {
        var plainId = "3f0a6a31-5c3b-4f52-9a2e-2b1f0c7d9e11";

        var result = transformer.transform(message(type, plainId, body), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).startsWith("urn:uuid:").doesNotContain(plainId);
    }

    private JsonObject message(String type, String id, Consumer<JsonObjectBuilder> body) {
        var builder = Json.createObjectBuilder()
                .add(ID, id)
                .add(TYPE, DSP_NAMESPACE.toIri(type))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "consumerPid")
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM), "providerPid");
        body.accept(builder);
        return TestJsonLd.expand(builder.build());
    }

    private static class Transformers implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            Consumer<JsonObjectBuilder> pidsOnly = builder -> { };
            Consumer<JsonObjectBuilder> offer = builder -> builder
                    .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_OFFER_TERM), Json.createObjectBuilder().add(ID, "offerId"));
            Consumer<JsonObjectBuilder> agreement = builder -> builder
                    .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_AGREEMENT_TERM), Json.createObjectBuilder()
                            .add(ID, "agreementId")
                            .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_TIMESTAMP_TERM), "2026-11-27T10:00:00Z"));
            Consumer<JsonObjectBuilder> callback = builder -> builder
                    .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), "https://consumer.example.com/protocol");
            Consumer<JsonObjectBuilder> accepted = builder -> builder
                    .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_EVENT_TYPE_TERM), DSP_NAMESPACE.toIri(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM));

            TypeManager typeManager = mock();
            when(typeManager.getMapper("test")).thenReturn(JsonMapperFactory.defaultJsonMapper());

            return Stream.of(
                    Arguments.of(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM, new JsonObjectToContractRequestMessageTransformer(DSP_NAMESPACE), offer),
                    Arguments.of(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM, new JsonObjectToContractOfferMessageTransformer(DSP_NAMESPACE), offer),
                    Arguments.of(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM, new JsonObjectToContractNegotiationEventMessageTransformer(DSP_NAMESPACE), accepted),
                    Arguments.of(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM, new JsonObjectToContractAgreementMessageTransformer(DSP_NAMESPACE), agreement),
                    Arguments.of(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM, new JsonObjectToContractAgreementVerificationMessageTransformer(DSP_NAMESPACE), pidsOnly),
                    Arguments.of(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM, new JsonObjectToContractNegotiationTerminationMessageTransformer(DSP_NAMESPACE), pidsOnly),
                    Arguments.of(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM, new JsonObjectToTransferRequestMessageTransformer(DSP_NAMESPACE), callback),
                    Arguments.of(DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM, new JsonObjectToTransferStartMessageTransformer(DSP_NAMESPACE), pidsOnly),
                    Arguments.of(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM, new JsonObjectToTransferCompletionMessageTransformer(DSP_NAMESPACE), pidsOnly),
                    Arguments.of(DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM, new JsonObjectToTransferSuspensionMessageTransformer(typeManager, "test", DSP_NAMESPACE), pidsOnly),
                    Arguments.of(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM, new JsonObjectToTransferTerminationMessageTransformer(DSP_NAMESPACE), pidsOnly)
            );
        }
    }
}
