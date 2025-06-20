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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;

/**
 * Creates a {@link ContractNegotiationTerminationMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractNegotiationTerminationMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, ContractNegotiationTerminationMessage> {

    public JsonObjectToContractNegotiationTerminationMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, ContractNegotiationTerminationMessage.class, namespace);
    }

    @Override
    public @Nullable ContractNegotiationTerminationMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractNegotiationTerminationMessage.Builder.newInstance();

        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }
        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM))
                    .report();
            return null;
        }

        var code = object.get(forNamespace(DSPACE_PROPERTY_CODE_TERM));
        if (code != null) { // optional property
            transformString(code, builder::code, context);
        }

        var reasons = object.get(forNamespace(DSPACE_PROPERTY_REASON_TERM));
        if (reasons != null) {  // optional property
            if (reasons instanceof JsonArray) {
                var array = (JsonArray) reasons;
                if (array.size() > 0) {
                    builder.rejectionReason(array.toString());
                }
            } else {
                context.problem()
                        .unexpectedType()
                        .type(forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM))
                        .property(forNamespace(DSPACE_PROPERTY_REASON_TERM))
                        .actual(reasons.getValueType().toString())
                        .expected(ARRAY)
                        .report();
            }
        }

        return builder.build();
    }
}
