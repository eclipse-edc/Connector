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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_CONSUMER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_POLICY;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_PROVIDER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_SIGNING_DATE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromContractAgreementDtoTransformer extends AbstractJsonLdTransformer<ContractAgreementDto, JsonObject> {
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementDtoTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractAgreementDto.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractAgreementDto dto, @NotNull TransformerContext context) {

        var bldr = jsonFactory.createObjectBuilder();
        bldr.add(TYPE, ContractAgreementDto.TYPE)
                .add(ID, dto.getId())
                .add(CONTRACT_AGREEMENT_ASSET_ID, dto.getAssetId())
                .add(CONTRACT_AGREEMENT_POLICY, context.transform(dto.getPolicy(), JsonObject.class))
                .add(CONTRACT_AGREEMENT_SIGNING_DATE, dto.getContractSigningDate())
                .add(CONTRACT_AGREEMENT_CONSUMER_ID, dto.getConsumerId())
                .add(CONTRACT_AGREEMENT_PROVIDER_ID, dto.getProviderId());
        return bldr.build();
    }
}
