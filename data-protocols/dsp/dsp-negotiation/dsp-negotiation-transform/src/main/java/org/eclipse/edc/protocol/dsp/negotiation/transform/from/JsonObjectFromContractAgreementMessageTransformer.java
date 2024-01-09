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
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.time.Instant.ofEpochSecond;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_ID;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_ID;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_TIMESTAMP;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;


/**
 * Creates a dspace:ContractAgreementMessage as {@link JsonObject} from {@link ContractAgreementMessage}.
 */
public class JsonObjectFromContractAgreementMessageTransformer extends AbstractJsonLdTransformer<ContractAgreementMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractAgreementMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractAgreementMessage agreementMessage, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, agreementMessage.getId())
                .add(TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .add(DSPACE_PROPERTY_PROVIDER_PID, agreementMessage.getProviderPid())
                .add(DSPACE_PROPERTY_CONSUMER_PID, agreementMessage.getConsumerPid())
                .add(DSPACE_PROPERTY_PROCESS_ID, agreementMessage.getProcessId());

        var agreement = agreementMessage.getContractAgreement();

        var policy = context.transform(agreement.getPolicy(), JsonObject.class);
        if (policy == null) {
            context.problem()
                    .nullProperty()
                    .type(ContractAgreementMessage.class)
                    .property("policy")
                    .report();
            return null;
        }

        // add the consumer id, provider id, and signing timestamp to the agreement
        var signing = ofEpochSecond(agreement.getContractSigningDate()).toString();

        var copiedPolicy = Json.createObjectBuilder(policy)
                .add(ID, agreement.getId())
                .add(DSPACE_PROPERTY_CONSUMER_ID, agreement.getConsumerId())
                .add(DSPACE_PROPERTY_PROVIDER_ID, agreement.getProviderId())
                .add(DSPACE_PROPERTY_TIMESTAMP, signing)
                .build();

        builder.add(DSPACE_PROPERTY_AGREEMENT, copiedPolicy);

        return builder.build();
    }

}
