/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.api.query.v5;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.catalog.api.query.TestUtil;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromCatalogV2025Transformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.range;
import static org.eclipse.edc.catalog.api.query.TestUtil.createCatalog;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class CatalogsApiV5ControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private static final String PATH = "/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/catalogs/request";
    private final QueryService queryService = mock();
    private final AuthorizationService authorizationService = mock();

    @Test
    void queryApi_whenEmptyResult() {
        when(authorizationService.authorize(any(), eq(PARTICIPANT_CONTEXT_ID), eq(PARTICIPANT_CONTEXT_ID), eq(ParticipantContext.class))).thenReturn(ServiceResult.success());
        when(queryService.getCatalog(any(), any())).thenReturn(ServiceResult.success(emptyList()));

        baseRequest()
                .contentType(JSON)
                .body(Json.createObjectBuilder().add(TYPE, "QuerySpec").build())
                .post(PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));

        verify(queryService).getCatalog(eq(PARTICIPANT_CONTEXT_ID), any());
    }

    @Test
    void queryApi_whenResultsReturned() {
        when(authorizationService.authorize(any(), eq(PARTICIPANT_CONTEXT_ID), eq(PARTICIPANT_CONTEXT_ID), eq(ParticipantContext.class))).thenReturn(ServiceResult.success());
        var catalogs = range(0, 3).mapToObj(i -> createCatalog("catalog-" + i)).toList();
        when(queryService.getCatalog(any(), any())).thenReturn(ServiceResult.success(catalogs));

        baseRequest()
                .contentType(JSON)
                .body(Json.createObjectBuilder().add(TYPE, "QuerySpec").build())
                .post(PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(3));
    }

    @Test
    void queryApi_whenNotAuthorized_shouldReturnError() {
        when(authorizationService.authorize(any(), eq(PARTICIPANT_CONTEXT_ID), eq(PARTICIPANT_CONTEXT_ID), eq(ParticipantContext.class)))
                .thenReturn(ServiceResult.unauthorized("not authorized"));

        baseRequest()
                .contentType(JSON)
                .body(Json.createObjectBuilder().add(TYPE, "QuerySpec").build())
                .post(PATH)
                .then()
                .statusCode(403);
    }

    @Test
    void queryApi_shouldUseEmptyQuerySpec_whenRequestBodyIsNull() {
        when(authorizationService.authorize(any(), eq(PARTICIPANT_CONTEXT_ID), eq(PARTICIPANT_CONTEXT_ID), eq(ParticipantContext.class))).thenReturn(ServiceResult.success());
        when(queryService.getCatalog(any(), any())).thenReturn(ServiceResult.success(emptyList()));

        baseRequest()
                .contentType(JSON)
                .post(PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));

        verify(queryService).getCatalog(eq(PARTICIPANT_CONTEXT_ID), eq(QuerySpec.none()));
    }

    @Override
    protected Object controller() {
        var typeTransformerRegistry = new TypeTransformerRegistryImpl(mock());
        var factory = Json.createBuilderFactory(Map.of());
        var mapper = new JacksonTypeManager();
        var participantIdMapper = new TestUtil.NoOpParticipantIdMapper();
        typeTransformerRegistry.register(new JsonObjectFromCatalogV2025Transformer(factory, new JacksonTypeManager(), JSON_LD, participantIdMapper, DSP_NAMESPACE_V_2025_1));
        typeTransformerRegistry.register(new JsonObjectFromDatasetTransformer(factory, mapper, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromDistributionTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectFromDataServiceTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        return new CatalogsApiV5Controller(queryService, typeTransformerRegistry, authorizationService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
