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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Creates a {@link ContractAgreementMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractAgreementMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractAgreementMessage> {

    public JsonObjectToContractAgreementMessageTransformer() {
        super(JsonObject.class, ContractAgreementMessage.class);
    }

    @Override
    public @Nullable ContractAgreementMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractAgreementMessage.Builder.newInstance();
        builder.protocol(DATASPACE_PROTOCOL_HTTP);
        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context);

        var policy = context.transform(object.getJsonObject(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT), Policy.class);
        if (policy == null) {
            context.reportProblem("Cannot transform to ContractAgreementMessage with null policy");
            return null;
        }

        var agreement = contractAgreement(object, policy, context);
        if (agreement == null) {
            context.reportProblem("Cannot transform to ContractAgreementMessage with null agreement");
            return null;
        }

        builder.contractAgreement(agreement);

        return builder.build();
    }

    private ContractAgreement contractAgreement(JsonObject object, Policy policy, TransformerContext context) {
        var builder = ContractAgreement.Builder.newInstance();
        builder.id(nodeId(object));
        builder.providerAgentId(""); // TODO
        builder.consumerAgentId(""); // TODO
        builder.policy(policy);
        builder.assetId(policy.getTarget());

        var timestamp = object.getString(DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP);
        try {
            builder.contractSigningDate(Long.parseLong(timestamp));
        } catch (NumberFormatException exception) {
            context.reportProblem(String.format("Cannot transform %s to long in ContractAgreementMessage", timestamp));
            return null;
        }

        return builder.build();
    }
}
