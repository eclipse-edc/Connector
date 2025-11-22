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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.transformer.JsonObjectToCallbackAddressTransformer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.validation.SuspendTransferValidator;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.validation.TerminateTransferValidator;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.validation.TransferRequestValidator;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to.JsonObjectToSuspendTransferTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to.JsonObjectToTerminateTransferTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to.JsonObjectToTransferRequestTransformer;
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
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3.TransferProcessApiV3.SuspendTransferSchema.SUSPEND_TRANSFER_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3.TransferProcessApiV3.TerminateTransferSchema.TERMINATE_TRANSFER_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3.TransferProcessApiV3.TransferProcessSchema.TRANSFER_PROCESS_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3.TransferProcessApiV3.TransferRequestSchema.TRANSFER_REQUEST_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3.TransferProcessApiV3.TransferStateSchema.TRANSFER_STATE_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CONTRACT_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CORRELATION_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CREATED_AT;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_DATA_DESTINATION;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_ERROR_DETAIL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_STATE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_STATE_TIMESTAMP;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState.TRANSFER_STATE_STATE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState.TRANSFER_STATE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferProcessApiTest {

    private final TypeManager typeManager = mock();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToTransferRequestTransformer());
        transformer.register(new JsonObjectToCallbackAddressTransformer());
        transformer.register(new JsonObjectToDataAddressTransformer());
        transformer.register(new JsonObjectToTerminateTransferTransformer());
        transformer.register(new JsonObjectToSuspendTransferTransformer());
        transformer.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        when(typeManager.getMapper("test")).thenReturn(objectMapper);
    }

    @Test
    void transferRequestExample() throws JsonProcessingException {
        var validator = TransferRequestValidator.instance(mock());

        var jsonObject = objectMapper.readValue(TRANSFER_REQUEST_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, TransferRequest.class))
                .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getCounterPartyAddress()).isNotBlank();
                            assertThat(transformed.getContractId()).isNotBlank();
                            assertThat(transformed.getProtocol()).isNotBlank();
                            assertThat(transformed.getDataDestination()).isNotNull();
                            assertThat(transformed.getPrivateProperties()).asInstanceOf(map(String.class, Object.class)).isNotEmpty();
                            assertThat(transformed.getCallbackAddresses()).asList().isNotEmpty();
                        }));
    }

    @Test
    void terminateTransferExample() throws JsonProcessingException {
        var validator = TerminateTransferValidator.instance();

        var jsonObject = objectMapper.readValue(TERMINATE_TRANSFER_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, TerminateTransfer.class))
                .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> assertThat(transformed.reason()).isNotBlank()));
    }

    @Test
    void suspendTransferExample() throws JsonProcessingException {
        var validator = SuspendTransferValidator.instance();

        var jsonObject = objectMapper.readValue(SUSPEND_TRANSFER_EXAMPLE, JsonObject.class);

        assertThat(jsonObject).isNotNull().extracting(jsonLd::expand).satisfies(expanded -> {
            assertThat(expanded).isSucceeded()
                    .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                    .extracting(e -> transformer.transform(e, SuspendTransfer.class))
                    .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                            .satisfies(transformed -> assertThat(transformed.reason()).isNotBlank()));
        });
    }

    @Test
    void transferProcessExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(TRANSFER_PROCESS_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getString(ID)).isNotBlank();
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(TRANSFER_PROCESS_TYPE);
            assertThat(content.getJsonArray(TRANSFER_PROCESS_CREATED_AT).getJsonObject(0).getJsonNumber(VALUE).longValue()).isGreaterThan(0);
            assertThat(content.getJsonArray(TRANSFER_PROCESS_CORRELATION_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_STATE).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_STATE_TIMESTAMP).getJsonObject(0).getJsonNumber(VALUE).longValue()).isGreaterThan(0);
            assertThat(content.getJsonArray(TRANSFER_PROCESS_ASSET_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_CONTRACT_ID).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_PRIVATE_PROPERTIES).getJsonObject(0)).asInstanceOf(map(String.class, Object.class)).isNotEmpty();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_TYPE_TYPE).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_ERROR_DETAIL).getJsonObject(0).getString(VALUE)).isNotBlank();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_DATA_DESTINATION).getJsonObject(0)).asInstanceOf(map(String.class, Object.class)).isNotEmpty();
            assertThat(content.getJsonArray(TRANSFER_PROCESS_CALLBACK_ADDRESSES)).asList().isNotEmpty();
        });
    }

    @Test
    void transferStateExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(TRANSFER_STATE_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(TRANSFER_STATE_TYPE);
            assertThat(content.getJsonArray(TRANSFER_STATE_STATE).getJsonObject(0).getString(VALUE)).isNotBlank();
        });
    }

}
