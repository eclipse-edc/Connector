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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

/**
 * Creates a {@link ContractAgreementVerificationMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractAgreementVerificationMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, ContractAgreementVerificationMessage> {

    public JsonObjectToContractAgreementVerificationMessageTransformer() {
        this(DSP_NAMESPACE_V_08);
    }

    public JsonObjectToContractAgreementVerificationMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, ContractAgreementVerificationMessage.class, namespace);
    }


    @Override
    public @Nullable ContractAgreementVerificationMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractAgreementVerificationMessage.Builder.newInstance();
        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }
        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM))
                    .report();
            return null;
        }

        return builder.build();
    }

}
