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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_STATE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_AGREED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_OFFERED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_REQUESTED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_TERMINATED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_VERIFIED;


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
                .add(ID, contractNegotiation.getCorrelationId())
                .add(TYPE, DSPACE_CONTRACT_NEGOTIATION)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, contractNegotiation.getCorrelationId())
                .add(DSPACE_NEGOTIATION_PROPERTY_STATE, state(contractNegotiation.getState(), context))
                .build();
    }

    private String state(Integer state, TransformerContext context) {
        var negotiationState = ContractNegotiationStates.from(state);
        if (negotiationState == null) {
            context.problem()
                    .nullProperty()
                    .type(ContractNegotiation.class)
                    .property(DSPACE_NEGOTIATION_PROPERTY_STATE)
                    .report();
            return null;
        }
        switch (negotiationState) {
            case REQUESTING:
            case REQUESTED:
                return DSPACE_NEGOTIATION_STATE_REQUESTED;
            case OFFERING:
            case OFFERED:
                return DSPACE_NEGOTIATION_STATE_OFFERED;
            case ACCEPTING:
            case ACCEPTED:
                return DSPACE_NEGOTIATION_STATE_ACCEPTED;
            case AGREEING:
            case AGREED:
                return DSPACE_NEGOTIATION_STATE_AGREED;
            case VERIFYING:
            case VERIFIED:
                return DSPACE_NEGOTIATION_STATE_VERIFIED;
            case FINALIZING:
            case FINALIZED:
                return DSPACE_NEGOTIATION_STATE_FINALIZED;
            case TERMINATING:
            case TERMINATED:
                return DSPACE_NEGOTIATION_STATE_TERMINATED;
            default:
                context.problem()
                        .unexpectedType()
                        .type(ContractNegotiation.class)
                        .property("state")
                        .actual(negotiationState.toString())
                        .expected(ContractNegotiationStates.class)
                        .report();
                return null;
        }
    }

}
