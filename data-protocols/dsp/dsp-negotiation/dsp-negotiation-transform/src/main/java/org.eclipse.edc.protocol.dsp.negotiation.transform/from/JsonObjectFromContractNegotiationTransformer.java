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
import org.eclipse.edc.jsonld.JsonLdKeywords;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.OFFERED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CHECKSUM;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_STATE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_AGREED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_OFFERED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_REQUESTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_TERMINATED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_VERIFIED;

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
    public @Nullable JsonObject transform(@NotNull ContractNegotiation object, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, object.getCorrelationId());
        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION);

        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, object.getCorrelationId());
        builder.add(DSPACE_NEGOTIATION_PROPERTY_STATE, state(object.getState(), context));
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CHECKSUM, object.getChecksum());

        return builder.build();
    }

    private String state(Integer state, TransformerContext context) {
        switch (ContractNegotiationStates.from(state)) {
            case REQUESTED:
                return DSPACE_NEGOTIATION_STATE_REQUESTED;
            case OFFERED:
                return DSPACE_NEGOTIATION_STATE_OFFERED;
            case ACCEPTED:
                return DSPACE_NEGOTIATION_STATE_ACCEPTED;
            case AGREED:
                return DSPACE_NEGOTIATION_STATE_AGREED;
            case VERIFIED:
                return DSPACE_NEGOTIATION_STATE_VERIFIED;
            case FINALIZED:
                return DSPACE_NEGOTIATION_STATE_FINALIZED;
            case TERMINATED:
                return DSPACE_NEGOTIATION_STATE_TERMINATED;
            default:
                context.reportProblem(String.format("Could not map state %s", state));
                return null;
        }
    }

}
