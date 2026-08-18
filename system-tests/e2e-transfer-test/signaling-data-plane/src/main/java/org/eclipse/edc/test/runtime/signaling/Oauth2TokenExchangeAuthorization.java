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

package org.eclipse.edc.test.runtime.signaling;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.registration.Authorization;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test-only counterpart of the control plane's {@code oauth2_token_exchange} authorization profile, used to exercise
 * the data plane side of the profile in end-to-end tests.
 * <p>
 * The workload credential path is passed as a {@code subjectTokenPath} attribute of the authorization profile, which
 * is a test convenience: a real data plane would read it from its own deployment configuration.
 */
public class Oauth2TokenExchangeAuthorization implements Authorization {

    private static final String BEARER = "Bearer ";
    private static final String GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String TOKEN_TYPE_JWT = "urn:ietf:params:oauth:token-type:jwt";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String type() {
        return "oauth2_token_exchange";
    }

    @Override
    public Result<String> authorizationHeader(AuthorizationProfile profile) {
        return Result.attempt(() -> {
            var subjectToken = Files.readString(Path.of(profile.stringAttribute("subjectTokenPath"))).trim();

            var form = new LinkedHashMap<String, String>();
            form.put("grant_type", GRANT_TYPE_TOKEN_EXCHANGE);
            form.put("subject_token", subjectToken);
            form.put("subject_token_type", TOKEN_TYPE_JWT);
            form.put("requested_token_type", TOKEN_TYPE_JWT);
            form.put("resource", profile.stringAttribute("resource"));
            form.put("audience", profile.stringAttribute("audience"));
            form.put("scope", profile.stringAttribute("scope"));

            var body = form.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                    .collect(Collectors.joining("&"));

            var request = HttpRequest.newBuilder(URI.create(profile.stringAttribute("tokenExchangeEndpoint")))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Token exchange failed with %d: %s".formatted(response.statusCode(), response.body()));
            }

            var parsed = objectMapper.readValue(response.body(), Map.class);
            return BEARER + parsed.get("access_token");
        });
    }

    /**
     * Mirrors the control plane's precedence: the caller identity is the {@code client_id} claim, falling back to
     * {@code sub} when the broker does not mint one.
     */
    @Override
    public Result<String> extractCallerId(String authorizationHeader) {
        return Result.attempt(() -> {
            var token = authorizationHeader.startsWith(BEARER) ? authorizationHeader.substring(BEARER.length()) : authorizationHeader;
            var segments = token.split("\\.");
            if (segments.length < 2) {
                throw new IllegalArgumentException("Not a JWT");
            }
            var claims = objectMapper.readValue(Base64.getUrlDecoder().decode(segments[1]), Map.class);
            var callerId = claims.getOrDefault("client_id", claims.get("sub"));
            if (!(callerId instanceof String identity)) {
                throw new IllegalArgumentException("Neither the JWT client_id nor the sub claim is a string");
            }
            return identity;
        });
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
