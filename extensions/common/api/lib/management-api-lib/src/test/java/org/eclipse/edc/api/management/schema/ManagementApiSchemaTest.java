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

package org.eclipse.edc.api.management.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.api.management.schema.ManagementApiSchema.ContractAgreementSchema.CONTRACT_AGREEMENT_EXAMPLE;
import static org.eclipse.edc.api.management.schema.ManagementApiSchema.ContractNegotiationSchema.CONTRACT_NEGOTIATION_EXAMPLE;
import static org.eclipse.edc.api.management.schema.ManagementApiSchema.PolicySchema.POLICY_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_CONSUMER_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_POLICY;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_PROVIDER_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_SIGNING_DATE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_AGREEMENT_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_CALLBACK_ADDR;
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
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManagementApiSchemaTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final TypeManager typeManager = mock();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToDataAddressTransformer());
        transformer.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        when(typeManager.getMapper("test")).thenReturn(objectMapper);
    }

    @Test
    void contractAgreementExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(CONTRACT_AGREEMENT_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(CONTRACT_AGREEMENT_TYPE);
            assertThat(content.getJsonArray(CONTRACT_AGREEMENT_ASSET_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_AGREEMENT_PROVIDER_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_AGREEMENT_CONSUMER_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_AGREEMENT_SIGNING_DATE).getJsonObject(0).getJsonNumber(VALUE).longValue()).isGreaterThan(0);
            assertThat(content.getJsonArray(CONTRACT_AGREEMENT_POLICY).getJsonObject(0)).isNotNull();
        });
    }

    @Test
    void contractNegotiationExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(CONTRACT_NEGOTIATION_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(CONTRACT_NEGOTIATION_TYPE);
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_CREATED_AT).getJsonObject(0).getJsonNumber(VALUE).longValue()).isGreaterThan(0);
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_NEG_TYPE).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_PROTOCOL).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_COUNTERPARTY_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_STATE).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_AGREEMENT_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_ERRORDETAIL).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(CONTRACT_NEGOTIATION_CALLBACK_ADDR)).asList().isNotEmpty();
        });
    }

    @Test
    void policyExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(POLICY_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(TYPE).getString(0)).isNotBlank();
            assertThat(content.getJsonArray(ODRL_PERMISSION_ATTRIBUTE).size()).isGreaterThan(0);
        });
    }

}
