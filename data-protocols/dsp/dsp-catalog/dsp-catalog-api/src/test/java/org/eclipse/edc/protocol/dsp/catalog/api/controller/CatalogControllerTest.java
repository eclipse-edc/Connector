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
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.catalog.spi.types.CatalogRequestMessage;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogControllerTest {

    private ObjectMapper mapper = mock(ObjectMapper.class);
    private IdentityService identityService = mock(IdentityService.class);
    private JsonLdTransformerRegistry transformerRegistry = mock(JsonLdTransformerRegistry.class);
    private String callbackAddress = "http://callback";

    private JsonObject request;
    private CatalogRequestMessage requestMessage;
    private String authHeader = "auth";

    private CatalogController controller;

    @BeforeEach
    void setUp() {
        controller = new CatalogController(mapper, identityService, transformerRegistry, callbackAddress);

        request = Json.createObjectBuilder()
                .add(TYPE, DSPACE_CATALOG_REQUEST_TYPE)
                .build();
        requestMessage = CatalogRequestMessage.Builder.newInstance().build();
    }

    @Test
    void getCatalog_returnCatalog() {
        var responseMap = Map.of("type", "catalog");
        
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.success(requestMessage));
        when(transformerRegistry.transform(any(Catalog.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()));
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));
        when(mapper.convertValue(any(JsonObject.class), eq(Map.class))).thenReturn(responseMap);

        var response = controller.getCatalog(request, authHeader);

        assertThat(response).isEqualTo(responseMap);
    }

    @Test
    void getCatalog_transformingRequestFails_throwException() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> controller.getCatalog(request, authHeader)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getCatalog_authenticationFails_throwException() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.success(requestMessage));
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> controller.getCatalog(request, authHeader)).isInstanceOf(AuthenticationFailedException.class);
    }

}
