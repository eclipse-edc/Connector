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

package org.eclipse.edc.protocol.dsp.http;

import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DspVirtualProfileDispatcherTest extends RestControllerTestBase {

    private static final String PARTICIPANT_ID = "participantId";
    private static final String PROFILE_ID = "profileId";
    private static final String PROTOCOL_VERSION = "2025-1";

    private final ParticipantProfileResolver profileResolver = mock();
    private final DspVirtualSubResourceLocator subResourceLocator = mock();
    private final TestSubResource subResource = new TestSubResource();

    @Test
    void shouldDispatchToRegisteredSubResource() {
        givenProfile();
        when(subResourceLocator.getSubResource("catalog", PROTOCOL_VERSION)).thenReturn(subResource);

        baseRequest()
                .get("/%s/%s/catalog/request".formatted(PARTICIPANT_ID, PROFILE_ID))
                .then()
                .log().ifError()
                .statusCode(200)
                .body(is("dispatched"));

        verify(profileResolver).resolve(PARTICIPANT_ID, PROFILE_ID);
        verify(subResourceLocator).getSubResource("catalog", PROTOCOL_VERSION);
    }

    @Test
    void shouldDispatchToRegisteredSubResource_forEachAllowedSegment() {
        givenProfile();
        when(subResourceLocator.getSubResource("negotiations", PROTOCOL_VERSION)).thenReturn(subResource);
        when(subResourceLocator.getSubResource("transfers", PROTOCOL_VERSION)).thenReturn(subResource);

        baseRequest().get("/%s/%s/negotiations/request".formatted(PARTICIPANT_ID, PROFILE_ID)).then().statusCode(200);
        baseRequest().get("/%s/%s/transfers/request".formatted(PARTICIPANT_ID, PROFILE_ID)).then().statusCode(200);
    }

    @Test
    void shouldReturnNotFound_whenSubResourceNotRegistered() {
        givenProfile();
        when(subResourceLocator.getSubResource("catalog", PROTOCOL_VERSION)).thenReturn(null);

        baseRequest()
                .get("/%s/%s/catalog/request".formatted(PARTICIPANT_ID, PROFILE_ID))
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturnNotFound_whenProfileNotResolvedForParticipant() {
        when(profileResolver.resolve(PARTICIPANT_ID, PROFILE_ID)).thenReturn(Optional.empty());

        baseRequest()
                .get("/%s/%s/catalog/request".formatted(PARTICIPANT_ID, PROFILE_ID))
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturnNotFound_whenSubResourceSegmentIsNotAllowed() {
        baseRequest()
                .get("/%s/%s/unknown/request".formatted(PARTICIPANT_ID, PROFILE_ID))
                .then()
                .statusCode(404);
    }

    @Override
    protected Object controller() {
        return new DspVirtualProfileDispatcher(profileResolver, subResourceLocator);
    }

    private void givenProfile() {
        var profile = new DataspaceProfileContext(PROFILE_ID,
                new ProtocolVersion(PROTOCOL_VERSION, "/" + PROTOCOL_VERSION, "binding"),
                mock(), mock(),
                new JsonLdNamespace("https://example.org/dspace/"),
                List.of("https://example.org/context.jsonld"));
        when(profileResolver.resolve(PARTICIPANT_ID, PROFILE_ID)).thenReturn(Optional.of(profile));
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }

    public static class TestSubResource {
        @GET
        @Path("request")
        public String handle() {
            return "dispatched";
        }
    }
}
