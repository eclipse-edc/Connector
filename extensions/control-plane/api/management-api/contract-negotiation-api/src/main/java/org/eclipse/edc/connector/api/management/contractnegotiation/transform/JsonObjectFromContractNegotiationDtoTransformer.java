/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.contractnegotiation.transform;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_AGREEMENT_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_CALLBACK_ADDR;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_ERRORDETAIL;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_NEG_TYPE;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_PROTOCOL;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_STATE;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromContractNegotiationDtoTransformer extends AbstractJsonLdTransformer<ContractNegotiationDto, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractNegotiationDtoTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractNegotiationDto.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractNegotiationDto dto, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(TYPE, CONTRACT_NEGOTIATION_TYPE)
                .add(ID, dto.getId())
                .add(CONTRACT_NEGOTIATION_NEG_TYPE, dto.getType().toString())
                .add(CONTRACT_NEGOTIATION_PROTOCOL, dto.getProtocol())
                .add(CONTRACT_NEGOTIATION_STATE, dto.getState())
                .add(CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR, dto.getCounterPartyAddress())
                .add(CONTRACT_NEGOTIATION_CALLBACK_ADDR, asArray(dto.getCallbackAddresses(), context));
        ofNullable(dto.getContractAgreementId()).ifPresent(s -> builder.add(CONTRACT_NEGOTIATION_AGREEMENT_ID, s));
        ofNullable(dto.getErrorDetail()).ifPresent(s -> builder.add(CONTRACT_NEGOTIATION_ERRORDETAIL, s));


        return builder.build();
    }

    private JsonArrayBuilder asArray(List<CallbackAddress> callbackAddresses, TransformerContext context) {
        var bldr = jsonFactory.createArrayBuilder();
        callbackAddresses.stream()
                .map(cba -> context.transform(cba, JsonObject.class))
                .forEach(bldr::add);

        return bldr;
    }
}
