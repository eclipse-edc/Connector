/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

/**
 * Creates a {@link JsonObject} from a {@link ContractAgreementVerificationMessage}.
 */
public class JsonObjectFromContractAgreementVerificationMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<ContractAgreementVerificationMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementVerificationMessageTransformer(JsonBuilderFactory jsonFactory, JsonLdNamespace namespace) {
        super(ContractAgreementVerificationMessage.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractAgreementVerificationMessage verificationMessage, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(ID, verificationMessage.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM))
                .add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), createId(jsonFactory, verificationMessage.getConsumerPid()))
                .add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), createId(jsonFactory, verificationMessage.getProviderPid()))
                .build();
    }
}
