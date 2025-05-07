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
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.time.Instant.ofEpochSecond;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNEE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_AGREEMENT_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_TIMESTAMP_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;


/**
 * Creates a dspace:ContractAgreementMessage as {@link JsonObject} from {@link ContractAgreementMessage}.
 */
public class JsonObjectFromContractAgreementMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<ContractAgreementMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementMessageTransformer(JsonBuilderFactory jsonFactory) {
        this(jsonFactory, DSP_NAMESPACE_V_08);
    }

    public JsonObjectFromContractAgreementMessageTransformer(JsonBuilderFactory jsonFactory, JsonLdNamespace namespace) {
        super(ContractAgreementMessage.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractAgreementMessage agreementMessage, @NotNull TransformerContext context) {
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

        var signing = ofEpochSecond(agreement.getContractSigningDate()).toString();

        var copiedPolicy = Json.createObjectBuilder(policy)
                .add(ID, agreement.getId())
                .add(ODRL_ASSIGNEE_ATTRIBUTE, agreement.getConsumerId())
                .add(ODRL_ASSIGNER_ATTRIBUTE, agreement.getProviderId())
                .add(forNamespace(DSPACE_PROPERTY_TIMESTAMP_TERM), signing)
                .build();

        return jsonFactory.createObjectBuilder()
                .add(ID, agreementMessage.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                .add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), agreementMessage.getProviderPid())
                .add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), agreementMessage.getConsumerPid())
                .add(forNamespace(DSPACE_PROPERTY_AGREEMENT_TERM), copiedPolicy)
                .build();
    }

}
