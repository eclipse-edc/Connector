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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_AGREED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_OFFERED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_TERMINATED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_VERIFIED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE;


/**
 * Creates a {@link JsonObject} from a {@link ContractNegotiation}.
 */
public class JsonObjectFromContractNegotiationTransformer extends AbstractJsonLdTransformer<ContractNegotiation, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractNegotiationTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractNegotiation.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractNegotiation contractNegotiation, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(ID, pidFor(contractNegotiation, contractNegotiation.getType()))
                .add(TYPE, DSPACE_TYPE_CONTRACT_NEGOTIATION)
                .add(DSPACE_PROPERTY_CONSUMER_PID, pidFor(contractNegotiation, ContractNegotiation.Type.CONSUMER))
                .add(DSPACE_PROPERTY_PROVIDER_PID, pidFor(contractNegotiation, ContractNegotiation.Type.PROVIDER))
                .add(DSPACE_PROPERTY_STATE, state(contractNegotiation.getState(), context))
                .build();
    }

    private String pidFor(@NotNull ContractNegotiation contractNegotiation, ContractNegotiation.Type type) {
        return contractNegotiation.getType() == type ? contractNegotiation.getId() : contractNegotiation.getCorrelationId();
    }

    private String state(Integer state, TransformerContext context) {
        var negotiationState = ContractNegotiationStates.from(state);
        if (negotiationState == null) {
            context.problem()
                    .nullProperty()
                    .type(ContractNegotiation.class)
                    .property(DSPACE_PROPERTY_STATE)
                    .report();
            return null;
        }

        return switch (negotiationState) {
            case REQUESTING, REQUESTED -> DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED;
            case OFFERING, OFFERED -> DSPACE_VALUE_NEGOTIATION_STATE_OFFERED;
            case ACCEPTING, ACCEPTED -> DSPACE_VALUE_NEGOTIATION_STATE_ACCEPTED;
            case AGREEING, AGREED -> DSPACE_VALUE_NEGOTIATION_STATE_AGREED;
            case VERIFYING, VERIFIED -> DSPACE_VALUE_NEGOTIATION_STATE_VERIFIED;
            case FINALIZING, FINALIZED -> DSPACE_VALUE_NEGOTIATION_STATE_FINALIZED;
            case TERMINATING, TERMINATED -> DSPACE_VALUE_NEGOTIATION_STATE_TERMINATED;
            default -> {
                context.problem()
                        .unexpectedType()
                        .type(ContractNegotiation.class)
                        .property("state")
                        .actual(negotiationState.toString())
                        .expected(ContractNegotiationStates.class)
                        .report();
                yield null;
            }
        };
    }

}
