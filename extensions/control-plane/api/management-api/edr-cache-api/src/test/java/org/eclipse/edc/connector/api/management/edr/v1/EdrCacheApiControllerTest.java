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

package org.eclipse.edc.connector.api.management.edr.v1;

import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public class EdrCacheApiControllerTest extends RestControllerTestBase {

    private static final String TEST_TRANSFER_PROCESS_ID = "test-transfer-process-id";
    private static final String TEST_TRANSFER_NEGOTIATION_ID = "test-cn-id";
    private static final String TEST_AGREEMENT_ID = "test-agreement-id";
    private static final String TEST_PROVIDER_ID = "test-provider-id";
    private static final String TEST_ASSET_ID = "test-asset-id";

    private final TypeTransformerRegistry transformerRegistry = mock();
    private final JsonObjectValidatorRegistry validator = mock();
    private final EndpointDataReferenceStore edrStore = mock();

    @Test
    void requestEdrEntries() {
        when(edrStore.query(any()))
                .thenReturn(StoreResult.success(List.of(createEdrEntry())));
        when(transformerRegistry.transform(isA(EndpointDataReferenceEntry.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createEdrEntryJson().build()));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/edrs/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));

        verify(edrStore).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(EndpointDataReferenceEntry.class), eq(JsonObject.class));
        verify(transformerRegistry).transform(isA(JsonObject.class), eq(QuerySpec.class));
    }


    @Test
    void getEdrEntryDataAddress() {

        var dataAddressType = "type";
        var dataAddress = DataAddress.Builder.newInstance().type(dataAddressType).build();
        when(edrStore.resolveByTransferProcess("transferProcessId"))
                .thenReturn(StoreResult.success(dataAddress));

        when(transformerRegistry.transform(isA(DataAddress.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createDataAddress(dataAddressType).build()));

        baseRequest()
                .contentType(JSON)
                .get("/edrs/transferProcessId/dataaddress")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("'%s'".formatted(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY), equalTo(dataAddressType));

        verify(edrStore).resolveByTransferProcess("transferProcessId");
        verify(transformerRegistry).transform(isA(DataAddress.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getEdrEntryDataAddress_whenNotFound() {

        when(edrStore.resolveByTransferProcess("transferProcessId"))
                .thenReturn(StoreResult.notFound("notFound"));


        baseRequest()
                .contentType(JSON)
                .get("/edrs/transferProcessId/dataaddress")
                .then()
                .log().ifError()
                .statusCode(404)
                .contentType(JSON);

        verify(edrStore).resolveByTransferProcess("transferProcessId");
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void removeEdrEntry() {
        when(edrStore.delete("transferProcessId"))
                .thenReturn(StoreResult.success(createEdrEntry()));

        baseRequest()
                .contentType(JSON)
                .delete("/edrs/transferProcessId")
                .then()
                .statusCode(204);
        verify(edrStore).delete("transferProcessId");
    }

    @Test
    void removeEdrEntry_whenNotFound() {
        when(edrStore.delete("transferProcessId"))
                .thenReturn(StoreResult.notFound("not found"));

        baseRequest()
                .contentType(JSON)
                .delete("/edrs/transferProcessId")
                .then()
                .statusCode(404);

        verify(edrStore).delete("transferProcessId");
    }

    @Override
    protected Object controller() {
        return new EdrCacheApiController(edrStore, transformerRegistry, validator, mock());
    }

    private JsonObjectBuilder createEdrEntryJson() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(ID, TEST_TRANSFER_PROCESS_ID)
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, TEST_TRANSFER_PROCESS_ID)
                .add(EDR_ENTRY_PROVIDER_ID, TEST_PROVIDER_ID)
                .add(EDR_ENTRY_CONTRACT_NEGOTIATION_ID, TEST_TRANSFER_NEGOTIATION_ID)
                .add(EDR_ENTRY_ASSET_ID, TEST_ASSET_ID)
                .add(EDR_ENTRY_AGREEMENT_ID, TEST_AGREEMENT_ID);
    }

    private JsonObjectBuilder createDataAddress(String type) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DataAddress.EDC_DATA_ADDRESS_TYPE)
                .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, type);
    }

    private EndpointDataReferenceEntry createEdrEntry() {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .agreementId(TEST_AGREEMENT_ID)
                .assetId(TEST_ASSET_ID)
                .providerId(TEST_PROVIDER_ID)
                .transferProcessId(TEST_TRANSFER_PROCESS_ID)
                .contractNegotiationId(TEST_TRANSFER_NEGOTIATION_ID)
                .build();

    }

    private JsonObjectBuilder createContextBuilder() {
        return createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }


    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v1")
                .when();
    }
}
