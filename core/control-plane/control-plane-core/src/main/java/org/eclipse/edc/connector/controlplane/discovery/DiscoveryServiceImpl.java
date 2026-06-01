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
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryService;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryUrlResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class DiscoveryServiceImpl implements DiscoveryService {

    private final EdcHttpClient httpClient;
    private final ParticipantProfileService participantProfileService;
    private final ObjectMapper objectMapper;
    private final List<DiscoveryUrlResolver> resolvers = new ArrayList<>();

    public DiscoveryServiceImpl(
            EdcHttpClient httpClient,
            ParticipantProfileService participantProfileService,
            ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.participantProfileService = participantProfileService;
        this.objectMapper = objectMapper;
    }


    @Override
    public ServiceResult<List<DiscoveryResponse>> discover(String participantContextId, DiscoveryRequest request) {

        var resolver = resolvers.stream()
                .filter(r -> r.canResolve(request))
                .findFirst()
                .orElse(null);

        if (resolver != null) {
            return resolveMatches(participantContextId, request, resolver);

        } else {
            return ServiceResult.badRequest("No resolver found for request: %s".formatted(request));
        }
    }

    private ServiceResult<List<DiscoveryResponse>> resolveMatches(String participantContextId, DiscoveryRequest request, DiscoveryUrlResolver resolver) {

        var wellKnownUrlResult = resolver.resolve(request);
        if (wellKnownUrlResult.failed()) {
            return ServiceResult.badRequest(wellKnownUrlResult.getFailureDetail());
        }
        var wellKnownUrl = wellKnownUrlResult.getContent();
        var versionsResult = fetchProtocolVersions(wellKnownUrl);
        if (versionsResult.failed()) {
            return ServiceResult.badRequest(versionsResult.getFailureDetail());
        }

        var localProfiles = participantProfileService.resolveAll(participantContextId);
        var matches = localProfiles.stream()
                .flatMap(profileMatcher(wellKnownUrl, versionsResult.getContent()))
                .toList();

        return ServiceResult.success(matches);
    }

    private Function<DataspaceProfileContext, Stream<? extends DiscoveryResponse>> profileMatcher(String wellKnownUrl, List<ProtocolVersion> versions) {
        return profile -> versions.stream()
                .filter(v -> v.version().equals(profile.protocolVersion().version()) &&
                        v.binding().equals(profile.protocolVersion().binding()))
                .map(v -> new DiscoveryResponse(
                        profile.name(),
                        v.version(),
                        new DiscoveryResponse.CounterParty(v.path(), wellKnownUrl),
                        v.binding()));
    }


    private ServiceResult<List<ProtocolVersion>> fetchProtocolVersions(String url) {
        var request = new Request.Builder().url(url).get().build();
        var result = httpClient.execute(request, response -> handleResponse(response, url));

        if (result.failed()) {
            return ServiceResult.badRequest(result.getFailureDetail());
        }
        return ServiceResult.success(result.getContent());
    }

    private Result<List<ProtocolVersion>> handleResponse(Response response, String url) {
        if (!response.isSuccessful()) {
            return Result.failure("Unexpected status %d fetching '%s'".formatted(response.code(), url));
        }
        try (var body = response.body()) {
            var versions = objectMapper.readValue(body.byteStream(), ProtocolVersions.class);
            return Result.success(versions.protocolVersions());
        } catch (IOException e) {
            return Result.failure("Failed to parse '%s': %s".formatted(url, e.getMessage()));
        }
    }

    @Override
    public void registerResolver(DiscoveryUrlResolver resolver) {
        resolvers.add(resolver);
    }
}
