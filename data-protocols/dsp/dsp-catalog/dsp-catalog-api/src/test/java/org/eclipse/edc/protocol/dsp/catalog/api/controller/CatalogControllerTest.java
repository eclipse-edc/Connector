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

package org.eclipse.edc.protocol.dsp.catalog.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.EdcApiException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.service.spi.result.ServiceResult.badRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogControllerTest {

    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    private final CatalogProtocolService service = mock(CatalogProtocolService.class);
    private final String callbackAddress = "http://callback";
    private final String authHeader = "auth";
    private final JsonLd jsonLdService = new TitaniumJsonLd(mock(Monitor.class));
    private JsonObject request;
    private CatalogRequestMessage requestMessage;
    private CatalogController controller;

    private static ClaimToken createToken() {
        return ClaimToken.Builder.newInstance().build();
    }

    @BeforeEach
    void setUp() {
        jsonLdService.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA);
        jsonLdService.registerNamespace(DCT_PREFIX, DCT_SCHEMA);
        jsonLdService.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA);
        jsonLdService.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA);
        controller = new CatalogController(mapper, identityService, transformerRegistry, callbackAddress, service, jsonLdService);

        request = Json.createObjectBuilder()
                .add(TYPE, DSPACE_CATALOG_REQUEST_TYPE)
                .build();
        requestMessage = CatalogRequestMessage.Builder.newInstance().protocol("protocol").build();
    }

    @Test
    void getCatalog_returnCatalog() {
        var responseMap = Map.of("type", "catalog");
        var token = createToken();
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.success(requestMessage));
        when(transformerRegistry.transform(any(Catalog.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()));
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(token));
        when(mapper.convertValue(any(JsonObject.class), eq(Map.class))).thenReturn(responseMap);
        when(service.getCatalog(any(), any()))
                .thenReturn(ServiceResult.success(Catalog.Builder.newInstance().build()));

        var response = controller.getCatalog(request, authHeader);

        assertThat(response).isEqualTo(responseMap);
        verify(service).getCatalog(requestMessage, token);
    }

    @Test
    void getCatalog_invalidTypeInRequest_throwException() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(createToken()));

        var invalidRequest = Json.createObjectBuilder()
                .add(TYPE, "not-a-catalog-request")
                .build();

        assertThatThrownBy(() -> controller.getCatalog(invalidRequest, authHeader)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getCatalog_transformingRequestFails_throwException() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(createToken()));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> controller.getCatalog(request, authHeader)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getCatalog_authenticationFails_throwException() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> controller.getCatalog(request, authHeader)).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void getCatalog_shouldThrowException_whenServiceCallFails() {
        when(service.getCatalog(any(), any())).thenReturn(badRequest("error"));
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(createToken()));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.success(requestMessage));

        assertThatThrownBy(() -> controller.getCatalog(request, authHeader)).isInstanceOf(EdcApiException.class);
        verify(service).getCatalog(any(), any());
    }
}
