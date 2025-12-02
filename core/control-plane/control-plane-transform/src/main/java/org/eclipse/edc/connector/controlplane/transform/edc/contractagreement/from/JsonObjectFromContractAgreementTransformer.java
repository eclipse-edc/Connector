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

package org.eclipse.edc.connector.controlplane.transform.edc.contractagreement.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_CONSUMER_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_POLICY;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_PROVIDER_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_SIGNING_DATE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromContractAgreementTransformer extends AbstractJsonLdTransformer<ContractAgreement, JsonObject> {
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractAgreementTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractAgreement.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractAgreement agreement, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, CONTRACT_AGREEMENT_TYPE)
                .add(ID, agreement.getId())
                .add(CONTRACT_AGREEMENT_ASSET_ID, agreement.getAssetId())
                .add(CONTRACT_AGREEMENT_ID, agreement.getAgreementId())
                .add(CONTRACT_AGREEMENT_POLICY, context.transform(agreement.getPolicy(), JsonObject.class))
                .add(CONTRACT_AGREEMENT_SIGNING_DATE, agreement.getContractSigningDate())
                .add(CONTRACT_AGREEMENT_CONSUMER_ID, agreement.getConsumerId())
                .add(CONTRACT_AGREEMENT_PROVIDER_ID, agreement.getProviderId())
                .build();
    }
}
