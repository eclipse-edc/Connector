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
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_ACCEPTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_AGREED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_FINALIZED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_OFFERED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_TERMINATED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_STATE_VERIFIED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE_TERM;


/**
 * Creates a {@link JsonObject} from a {@link ContractNegotiation}.
 */
public class JsonObjectFromContractNegotiationTransformer extends AbstractNamespaceAwareJsonLdTransformer<ContractNegotiation, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractNegotiationTransformer(JsonBuilderFactory jsonFactory) {
        this(jsonFactory, DSPACE_SCHEMA);
    }

    public JsonObjectFromContractNegotiationTransformer(JsonBuilderFactory jsonFactory, String namespace) {
        super(ContractNegotiation.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractNegotiation contractNegotiation, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(ID, pidFor(contractNegotiation, contractNegotiation.getType()))
                .add(TYPE, DSPACE_TYPE_CONTRACT_NEGOTIATION_IRI)
                .add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), pidFor(contractNegotiation, ContractNegotiation.Type.CONSUMER))
                .add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), pidFor(contractNegotiation, ContractNegotiation.Type.PROVIDER))
                .add(forNamespace(DSPACE_PROPERTY_STATE_TERM), state(contractNegotiation.getState(), context))
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
                    .property(forNamespace(DSPACE_PROPERTY_STATE_TERM))
                    .report();
            return null;
        }

        return switch (negotiationState) {
            case REQUESTING, REQUESTED -> forNamespace(DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED_TERM);
            case OFFERING, OFFERED -> forNamespace(DSPACE_VALUE_NEGOTIATION_STATE_OFFERED_TERM);
            case ACCEPTING, ACCEPTED -> forNamespace(DSPACE_VALUE_NEGOTIATION_STATE_ACCEPTED_TERM);
            case AGREEING, AGREED -> forNamespace(DSPACE_VALUE_NEGOTIATION_STATE_AGREED_TERM);
            case VERIFYING, VERIFIED -> forNamespace(DSPACE_VALUE_NEGOTIATION_STATE_VERIFIED_TERM);
            case FINALIZING, FINALIZED -> forNamespace(DSPACE_VALUE_NEGOTIATION_STATE_FINALIZED_TERM);
            case TERMINATING, TERMINATED -> forNamespace(DSPACE_VALUE_NEGOTIATION_STATE_TERMINATED_TERM);
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
