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

package org.eclipse.edc.connector.controlplane.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryUrlResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static okhttp3.MediaType.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscoveryServiceImplTest {

    private static final String PARTICIPANT = "participant-id";
    private static final String COUNTER_PARTY_BASE = "https://counter-party.example";

    private final EdcHttpClient httpClient = mock();
    private final ParticipantProfileService participantProfileService = mock();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DiscoveryServiceImpl service = new DiscoveryServiceImpl(httpClient, participantProfileService, objectMapper);

    private static Response httpResponse(Request request, int code, String body) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(code >= 200 && code < 300 ? "OK" : "ERR")
                .body(ResponseBody.create(body, parse("application/json")))
                .build();
    }

    @Test
    void shouldFailWhenNoResolverIsRegistered() {
        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        verify(httpClient, never()).execute(any(Request.class), any(Function.class));
    }

    @Test
    void shouldFailWhenNoResolverMatchesTheRequest() {
        service.registerResolver(resolver(false, ServiceResult.success("ignored")));

        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        verify(httpClient, never()).execute(any(Request.class), any(Function.class));
    }

    @Test
    void shouldUseTheFirstResolverThatCanResolve() {
        var matchingResolver = resolver(true, ServiceResult.success(COUNTER_PARTY_BASE));
        var skippedResolver = resolver(true, ServiceResult.success("https://wrong.example"));
        service.registerResolver(matchingResolver);
        service.registerResolver(skippedResolver);
        when(participantProfileService.resolveAll(PARTICIPANT)).thenReturn(List.of());
        stubHttpVersions("{\"protocolVersions\":[]}");

        var request = new DiscoveryRequest(null, COUNTER_PARTY_BASE);
        var result = service.discover(PARTICIPANT, request);

        assertThat(result.succeeded()).isTrue();
        verify(matchingResolver).resolve(request);
        verify(skippedResolver, never()).resolve(any());
    }

    @Test
    void shouldReturnMatchesForOverlappingProfiles() {
        service.registerResolver(resolver(true, ServiceResult.success(COUNTER_PARTY_BASE)));
        when(participantProfileService.resolveAll(PARTICIPANT)).thenReturn(List.of(
                profile("dsp-2025-1", new ProtocolVersion("2025-1", "/local-2025", "http")),
                profile("dsp-2024-1", new ProtocolVersion("2024-1", "/local-2024", "http"))
        ));
        stubHttpVersions("""
                {"protocolVersions":[
                    {"version":"2025-1","path":"/remote-2025","binding":"http"},
                    {"version":"2024-1","path":"/remote-2024","binding":"http"}
                ]}""");

        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(2)
                .anySatisfy(m -> {
                    assertThat(m.profile()).isEqualTo("dsp-2025-1");
                    assertThat(m.version()).isEqualTo("2025-1");
                    assertThat(m.counterPartyPath()).isEqualTo("/remote-2025");
                    assertThat(m.binding()).isEqualTo("http");
                })
                .anySatisfy(m -> {
                    assertThat(m.profile()).isEqualTo("dsp-2024-1");
                    assertThat(m.version()).isEqualTo("2024-1");
                    assertThat(m.counterPartyPath()).isEqualTo("/remote-2024");
                });
    }

    @Test
    void shouldFilterByBothVersionAndBinding() {
        service.registerResolver(resolver(true, ServiceResult.success(COUNTER_PARTY_BASE)));
        when(participantProfileService.resolveAll(PARTICIPANT)).thenReturn(List.of(
                profile("dsp-2025-1", new ProtocolVersion("2025-1", "/local", "http")),
                profile("dsp-other", new ProtocolVersion("9999-1", "/x", "http"))
        ));
        stubHttpVersions("""
                {"protocolVersions":[
                    {"version":"2025-1","path":"/remote-http","binding":"http"},
                    {"version":"2025-1","path":"/remote-grpc","binding":"grpc"}
                ]}""");

        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.getContent()).singleElement().satisfies(m -> {
            assertThat(m.profile()).isEqualTo("dsp-2025-1");
            assertThat(m.binding()).isEqualTo("http");
            assertThat(m.counterPartyPath()).isEqualTo("/remote-http");
        });
    }
    
    @Test
    void shouldFailWhenResolverReturnsFailure() {
        service.registerResolver(resolver(true, ServiceResult.notFound("no DataService endpoint")));

        var result = service.discover(PARTICIPANT, new DiscoveryRequest("did:web:x", null));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        verify(httpClient, never()).execute(any(Request.class), any(Function.class));
    }

    @Test
    void shouldFailWhenWellKnownReturnsErrorStatus() {
        service.registerResolver(resolver(true, ServiceResult.success(COUNTER_PARTY_BASE)));
        when(httpClient.execute(any(Request.class), any(Function.class))).thenAnswer(invocation -> {
            Function<Response, Result<?>> fn = invocation.getArgument(1);
            return fn.apply(httpResponse(invocation.getArgument(0), 500, "boom"));
        });

        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        verify(participantProfileService, never()).resolveAll(any());
    }

    @Test
    void shouldFailWhenWellKnownReturnsMalformedJson() {
        service.registerResolver(resolver(true, ServiceResult.success(COUNTER_PARTY_BASE)));
        stubHttpVersions("not-json");

        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        verify(participantProfileService, never()).resolveAll(any());
    }

    @Test
    void shouldReturnEmptyWhenNoLocalProfilesConfigured() {
        service.registerResolver(resolver(true, ServiceResult.success(COUNTER_PARTY_BASE)));
        when(participantProfileService.resolveAll(PARTICIPANT)).thenReturn(List.of());
        stubHttpVersions("""
                {"protocolVersions":[{"version":"2025-1","path":"/remote","binding":"http"}]}""");

        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenCounterPartyAdvertisesNoVersions() {
        service.registerResolver(resolver(true, ServiceResult.success(COUNTER_PARTY_BASE)));
        when(participantProfileService.resolveAll(PARTICIPANT)).thenReturn(List.of(
                profile("dsp-2025-1", new ProtocolVersion("2025-1", "/local", "http"))
        ));
        stubHttpVersions("{\"protocolVersions\":[]}");

        var result = service.discover(PARTICIPANT, new DiscoveryRequest(null, COUNTER_PARTY_BASE));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEmpty();
    }

    private DiscoveryUrlResolver resolver(boolean canResolve, ServiceResult<String> result) {
        var resolver = mock(DiscoveryUrlResolver.class);
        when(resolver.canResolve(any())).thenReturn(canResolve);
        when(resolver.resolve(any())).thenReturn(result);
        return resolver;
    }

    private DataspaceProfileContext profile(String name, ProtocolVersion version) {
        return new DataspaceProfileContext(name, version, mock(), mock(),
                new JsonLdNamespace("https://example.org/ns/"), List.of());
    }

    private void stubHttpVersions(String json) {
        when(httpClient.execute(any(Request.class), any(Function.class))).thenAnswer(invocation -> {
            Function<Response, Result<?>> fn = invocation.getArgument(1);
            return fn.apply(httpResponse(invocation.getArgument(0), 200, json));
        });
    }
}
