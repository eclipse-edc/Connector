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
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerService;
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
import static org.eclipse.edc.connector.controlplane.partner.spi.Partner.EDC_PARTNER_TYPE_TERM;
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
class PartnerApiV5ControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_CONTEXT_ID = "pc";
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final PartnerService service = mock();
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
        var partner = partner();
        when(transformerRegistry.transform(any(JsonObject.class), eq(Partner.class))).thenReturn(Result.success(partner));
        when(service.create(any())).thenReturn(ServiceResult.success(partner));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_TYPE_TERM).add("identity", "did:web:acme").build())
                .post("/partners")
                .then()
                .statusCode(200)
                .body(ID, is("partner-id"));

        verify(service).create(argThat(p -> PARTICIPANT_CONTEXT_ID.equals(p.getParticipantContextId())));
    }

    @Test
    void create_shouldReturn409_whenConflict() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(Partner.class))).thenReturn(Result.success(partner()));
        when(service.create(any())).thenReturn(ServiceResult.conflict("exists"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_TYPE_TERM).add("identity", "did:web:acme").build())
                .post("/partners")
                .then()
                .statusCode(409);
    }

    @Test
    void create_shouldReturn400_whenGroupUnknown() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(Partner.class))).thenReturn(Result.success(partner()));
        when(service.create(any())).thenReturn(ServiceResult.badRequest("unknown group"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_TYPE_TERM).add("identity", "did:web:acme").build())
                .post("/partners")
                .then()
                .statusCode(400);
    }

    @Test
    void create_shouldReturn403_whenUnauthorized() {
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.unauthorized("nope"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_TYPE_TERM).add("identity", "did:web:acme").build())
                .post("/partners")
                .then()
                .statusCode(403);

        verify(service, never()).create(any());
    }

    @Test
    void get_shouldReturnPartner() {
        when(service.findById(PARTICIPANT_CONTEXT_ID, "partner-id")).thenReturn(ServiceResult.success(partner()));
        when(transformerRegistry.transform(isA(Partner.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().add(ID, "partner-id").build()));

        baseRequest()
                .get("/partners/partner-id")
                .then()
                .statusCode(200)
                .body(ID, is("partner-id"));
    }

    @Test
    void get_shouldReturn404_whenNotFound() {
        when(service.findById(PARTICIPANT_CONTEXT_ID, "partner-id")).thenReturn(ServiceResult.notFound("missing"));

        baseRequest()
                .get("/partners/partner-id")
                .then()
                .statusCode(404);
    }

    @Test
    void query_shouldAppendParticipantContextFilter() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(partner())));
        when(transformerRegistry.transform(isA(Partner.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().add(ID, "partner-id").build()));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, "QuerySpec").build())
                .post("/partners/request")
                .then()
                .statusCode(200)
                .body("size()", is(1));

        verify(service).search(argThat(q -> q.getFilterExpression().contains(filterByParticipantContextId(PARTICIPANT_CONTEXT_ID))));
    }

    @Test
    void update_shouldReturn204() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(Partner.class))).thenReturn(Result.success(partner()));
        when(service.update(any())).thenReturn(ServiceResult.success(partner()));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_PARTNER_TYPE_TERM).add("identity", "did:web:acme").build())
                .put("/partners/partner-id")
                .then()
                .statusCode(204);

        verify(service).update(argThat(p -> "partner-id".equals(p.getId()) && PARTICIPANT_CONTEXT_ID.equals(p.getParticipantContextId())));
    }

    @Test
    void update_shouldReturn400_whenIdMismatch() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(Partner.class))).thenReturn(Result.success(partner()));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(ID, "partner-id").add(TYPE, EDC_PARTNER_TYPE_TERM).add("identity", "did:web:acme").build())
                .put("/partners/another-id")
                .then()
                .statusCode(400);

        verify(service, never()).update(any());
    }

    @Test
    void delete_shouldReturn204() {
        when(service.delete(PARTICIPANT_CONTEXT_ID, "partner-id")).thenReturn(ServiceResult.success(partner()));

        baseRequest()
                .delete("/partners/partner-id")
                .then()
                .statusCode(204);
    }

    @Test
    void delete_shouldReturn404_whenNotFound() {
        when(service.delete(PARTICIPANT_CONTEXT_ID, "partner-id")).thenReturn(ServiceResult.notFound("missing"));

        baseRequest()
                .delete("/partners/partner-id")
                .then()
                .statusCode(404);
    }

    @Override
    protected Object controller() {
        return new PartnerApiV5Controller(service, transformerRegistry, monitor, authorizationService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v5/participants/" + PARTICIPANT_CONTEXT_ID)
                .when();
    }

    private Partner partner() {
        return Partner.Builder.newInstance().id("partner-id").participantContextId(PARTICIPANT_CONTEXT_ID).identity("did:web:acme").createdAt(1234).build();
    }
}
