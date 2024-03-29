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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToContractOfferDescriptionTransformer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToContractOfferDescriptionTransformerTest {

    private final JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private JsonObjectToContractOfferDescriptionTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractOfferDescriptionTransformer();
    }

    @Test
    void transform() {
        var jsonObject = Json.createObjectBuilder()
                .add(TYPE, ContractOfferDescription.CONTRACT_OFFER_DESCRIPTION_TYPE)
                .add(OFFER_ID, "test-offer-id")
                .add(ASSET_ID, "test-asset")
                .add(POLICY, createPolicy())
                .build();

        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Policy.class))).thenReturn(Policy.Builder.newInstance().build());

        var result = transformer.transform(jsonLd.expand(jsonObject).getContent(), context);

        assertThat(result).isNotNull();
        assertThat(result.getOfferId()).isEqualTo("test-offer-id");
        assertThat(result.getAssetId()).isEqualTo("test-asset");
        assertThat(result.getPolicy()).isNotNull();

    }

    private JsonObject createPolicy() {
        var permissionJson = getJsonObject("permission");
        var prohibitionJson = getJsonObject("prohibition");
        var dutyJson = getJsonObject("duty");
        return Json.createObjectBuilder()
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .add(ODRL_PERMISSION_ATTRIBUTE, permissionJson)
                .add(ODRL_PROHIBITION_ATTRIBUTE, prohibitionJson)
                .add(ODRL_OBLIGATION_ATTRIBUTE, dutyJson)
                .build();
    }

    private JsonObject getJsonObject(String type) {
        return Json.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
}
