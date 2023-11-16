/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.core.defaults;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.identitytrust.CredentialServiceClient;
import org.eclipse.edc.identitytrust.VcConstants;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;
import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationQuery;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationResponse;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class DefaultCredentialServiceClient implements CredentialServiceClient {
    private final EdcHttpClient httpClient;
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper objectMapper;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLd;
    private final Monitor monitor;

    public DefaultCredentialServiceClient(EdcHttpClient httpClient, JsonBuilderFactory jsonFactory, ObjectMapper jsonLdMapper, TypeTransformerRegistry transformerRegistry, JsonLd jsonLd, Monitor monitor) {
        this.httpClient = httpClient;
        this.jsonFactory = jsonFactory;
        this.objectMapper = jsonLdMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        this.transformerRegistry = transformerRegistry;
        this.jsonLd = jsonLd;
        this.monitor = monitor;
    }

    @Override
    public Result<List<VerifiablePresentationContainer>> requestPresentation(String credentialServiceUrl, String selfIssuedTokenJwt, List<String> scopes) {
        var query = createPresentationQuery(scopes);

        try {
            var requestJson = objectMapper.writeValueAsString(query);
            var request = new Request.Builder()
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .url(credentialServiceUrl)
                    .addHeader("Authorization", "%s".formatted(selfIssuedTokenJwt))
                    .build();

            var response = httpClient.execute(request);

            var body = "";
            if (response.body() != null) {
                body = response.body().string();
            }

            if (response.isSuccessful() && response.body() != null) {
                var presentationResponse = objectMapper.readValue(body, PresentationResponse.class);
                return parseResponse(presentationResponse);
            }
            return failure("Presentation Query failed: HTTP %s, message: %s".formatted(response.code(), body));

        } catch (IOException e) {
            monitor.warning("Error requesting VP", e);
            return failure("Error requesting VP: %s".formatted(e.getMessage()));
        }

    }

    private Result<List<VerifiablePresentationContainer>> parseResponse(PresentationResponse presentationResponse) throws IOException {
        var vpResults = Stream.of(presentationResponse.vpToken())
                .map(this::parseVpToken)
                .toList();

        if (vpResults.stream().anyMatch(AbstractResult::failed)) {
            return failure("One or more VP tokens could not be parsed. Details: %s".formatted(vpResults.stream().filter(Result::failed).map(AbstractResult::getFailureDetail).collect(Collectors.joining(","))));
        }

        return success(vpResults.stream().map(AbstractResult::getContent).toList());
    }

    private Result<VerifiablePresentationContainer> parseVpToken(Object vpObj) {
        if (vpObj instanceof String) { // JWT VP
            return parseJwtVp(vpObj.toString());
        } else if (vpObj instanceof Map) { // LDP VP
            return parseLdpVp(vpObj);
        } else {
            return failure("Unknown VP format: " + vpObj.getClass());
        }
    }

    private Result<VerifiablePresentationContainer> parseLdpVp(Object vpObj) {
        var jsonObj = objectMapper.convertValue(vpObj, JsonObject.class);
        var rawStr = jsonObj.toString();

        return jsonLd.expand(jsonObj)
                .compose(expanded -> transformerRegistry.transform(expanded, VerifiablePresentation.class))
                .map(vp -> new VerifiablePresentationContainer(rawStr, CredentialFormat.JSON_LD, vp));
    }

    private Result<VerifiablePresentationContainer> parseJwtVp(String rawJwt) {
        try {
            var jwt = SignedJWT.parse(rawJwt);
            var claims = jwt.getJWTClaimsSet();
            var vp = claims.getClaim("vp");
            //todo: parse JWT VP
            return success(new VerifiablePresentationContainer(rawJwt, CredentialFormat.JWT, null));
        } catch (ParseException e) {
            monitor.warning("Failed to parse JWT VP", e);
            return failure("Failed to parse JWT VP: %s".formatted(e.getMessage()));
        }
    }

    private JsonObject createPresentationQuery(List<String> scopes) {
        var scopeArray = jsonFactory.createArrayBuilder();
        scopes.forEach(scopeArray::add);
        return jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, jsonFactory.createArrayBuilder()
                        .add(VcConstants.PRESENTATION_EXCHANGE_URL)
                        .add(VcConstants.IATP_CONTEXT_URL))
                .add(JsonLdKeywords.TYPE, PresentationQuery.PRESENTATION_QUERY_TYPE_PROPERTY)
                .add("scope", scopeArray.build())
                .build();
    }
}
