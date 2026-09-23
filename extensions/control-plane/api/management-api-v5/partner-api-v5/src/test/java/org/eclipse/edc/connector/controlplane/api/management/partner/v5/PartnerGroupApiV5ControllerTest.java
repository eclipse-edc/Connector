/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.partner.v5;

import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerGroupService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_CREATED_AT;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_TYPE;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_TYPE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class PartnerGroupApiV5ControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_CONTEXT_ID = "pc";
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final PartnerGroupService service = mock();
    private final AuthorizationService authorizationService = mock();

    @BeforeEach
    void setup() {
        when(transformerRegistry.transform(isA(IdResponse.class), eq(JsonObject.class))).thenAnswer(a -> {
            var idResponse = (IdResponse) a.getArgument(0);
            return Result.success(createObjectBuilder()
                    .add(TYPE, ID_RESPONSE_TYPE)
                    .add(ID, idResponse.getId())
                    .add(ID_RESPONSE_CREATED_AT, idResponse.getCreatedAt())
                    .build());
        });
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void create_shouldReturnIdResponse() {
        var group = group();
        when(transformerRegistry.transform(any(JsonObject.class), eq(PartnerGroup.class))).thenReturn(Result.success(group));
        when(service.create(any())).thenReturn(ServiceResult.success(group));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_GROUP_TYPE_TERM).add("name", "gold").build())
                .post("/partnergroups")
                .then()
                .statusCode(200)
                .body(ID, is("gold"));

        verify(service).create(argThat(g -> PARTICIPANT_CONTEXT_ID.equals(g.getParticipantContextId())));
    }

    @Test
    void create_shouldReturn403_whenUnauthorized() {
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.unauthorized("nope"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_GROUP_TYPE_TERM).add("name", "gold").build())
                .post("/partnergroups")
                .then()
                .statusCode(403);

        verify(service, never()).create(any());
    }

    @Test
    void get_shouldReturnGroup() {
        when(service.findById(PARTICIPANT_CONTEXT_ID, "gold")).thenReturn(ServiceResult.success(group()));
        when(transformerRegistry.transform(isA(PartnerGroup.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().add(ID, "gold").build()));

        baseRequest()
                .get("/partnergroups/gold")
                .then()
                .statusCode(200)
                .body(ID, is("gold"));
    }

    @Test
    void get_shouldReturn404_whenNotFound() {
        when(service.findById(PARTICIPANT_CONTEXT_ID, "gold")).thenReturn(ServiceResult.notFound("missing"));

        baseRequest()
                .get("/partnergroups/gold")
                .then()
                .statusCode(404);
    }

    @Test
    void query_shouldAppendParticipantContextFilter() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(group())));
        when(transformerRegistry.transform(isA(PartnerGroup.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().add(ID, "gold").build()));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, "QuerySpec").build())
                .post("/partnergroups/request")
                .then()
                .statusCode(200)
                .body("size()", is(1));

        verify(service).search(argThat(q -> q.getFilterExpression().contains(filterByParticipantContextId(PARTICIPANT_CONTEXT_ID))));
    }

    @Test
    void update_shouldReturn204() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(PartnerGroup.class))).thenReturn(Result.success(group()));
        when(service.update(any())).thenReturn(ServiceResult.success(group()));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_GROUP_TYPE_TERM).add("name", "gold").build())
                .put("/partnergroups/gold")
                .then()
                .statusCode(204);
    }

    @Test
    void delete_shouldReturn409_whenReferenced() {
        when(service.delete(PARTICIPANT_CONTEXT_ID, "gold")).thenReturn(ServiceResult.conflict("referenced"));

        baseRequest()
                .delete("/partnergroups/gold")
                .then()
                .statusCode(409);
    }

    @Test
    void delete_shouldReturn204() {
        when(service.delete(PARTICIPANT_CONTEXT_ID, "gold")).thenReturn(ServiceResult.success(group()));

        baseRequest()
                .delete("/partnergroups/gold")
                .then()
                .statusCode(204);
    }

    @Override
    protected Object controller() {
        return new PartnerGroupApiV5Controller(service, transformerRegistry, monitor, authorizationService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v5/participants/" + PARTICIPANT_CONTEXT_ID)
                .when();
    }

    private PartnerGroup group() {
        return PartnerGroup.Builder.newInstance().id("gold").participantContextId(PARTICIPANT_CONTEXT_ID).name("Gold").createdAt(1234).build();
    }
}
