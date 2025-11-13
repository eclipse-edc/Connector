/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_AGREEMENT_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_CALLBACK_ADDR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_CORRELATION_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_COUNTERPARTY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_CREATED_AT;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_ERRORDETAIL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_NEG_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_STATE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromContractNegotiationTransformer extends AbstractJsonLdTransformer<ContractNegotiation, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractNegotiationTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractNegotiation.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractNegotiation contractNegotiation, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();

        var callbackAddresses = contractNegotiation.getCallbackAddresses().stream()
                .map(callbackAddress -> context.transform(callbackAddress, JsonObject.class))
                .collect(toJsonArray());

        builder.add(TYPE, CONTRACT_NEGOTIATION_TYPE)
                .add(ID, contractNegotiation.getId())
                .add(CONTRACT_NEGOTIATION_NEG_TYPE, contractNegotiation.getType().toString())
                .add(CONTRACT_NEGOTIATION_PROTOCOL, contractNegotiation.getProtocol())
                .add(CONTRACT_NEGOTIATION_STATE, ContractNegotiationStates.from(contractNegotiation.getState()).name())
                .add(CONTRACT_NEGOTIATION_COUNTERPARTY_ID, contractNegotiation.getCounterPartyId())
                .add(CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR, contractNegotiation.getCounterPartyAddress())
                .add(CONTRACT_NEGOTIATION_CALLBACK_ADDR, callbackAddresses)
                .add(CONTRACT_NEGOTIATION_CREATED_AT, contractNegotiation.getCreatedAt());

        ofNullable(contractNegotiation.getLastContractOffer()).ifPresent(contractOffer -> builder.add(CONTRACT_NEGOTIATION_ASSET_ID, contractOffer.getAssetId()));
        ofNullable(contractNegotiation.getContractAgreement()).map(ContractAgreement::getId).ifPresent(s -> builder.add(CONTRACT_NEGOTIATION_AGREEMENT_ID, s));
        ofNullable(contractNegotiation.getCorrelationId()).ifPresent(correlationId -> builder.add(CONTRACT_NEGOTIATION_CORRELATION_ID, correlationId));
        ofNullable(contractNegotiation.getErrorDetail()).ifPresent(s -> builder.add(CONTRACT_NEGOTIATION_ERRORDETAIL, s));

        return builder.build();
    }

}
