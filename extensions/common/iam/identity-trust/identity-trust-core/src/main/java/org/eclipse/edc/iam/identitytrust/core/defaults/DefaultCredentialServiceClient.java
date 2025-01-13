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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.identitytrust.core.defaults;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.spi.DcpConstants;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class DefaultCredentialServiceClient implements CredentialServiceClient {
    public static final String PRESENTATION_ENDPOINT = "/presentations/query";
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
    public Result<List<VerifiablePresentationContainer>> requestPresentation(String credentialServiceBaseUrl, String selfIssuedTokenJwt, List<String> scopes) {
        var query = createPresentationQuery(scopes);

        var url = credentialServiceBaseUrl + PRESENTATION_ENDPOINT;

        try {
            var requestJson = objectMapper.writeValueAsString(query);
            var request = new Request.Builder()
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .url(url)
                    .addHeader("Authorization", "Bearer %s".formatted(selfIssuedTokenJwt))
                    .build();

            var response = httpClient.execute(request);

            var body = "";
            if (response.body() != null) {
                body = response.body().string();
            }

            if (response.isSuccessful() && response.body() != null) {
                var presentationResponse = objectMapper.readValue(body, JsonObject.class);
                return parseResponse(presentationResponse);
            }
            return failure("Presentation Query failed: HTTP %s, message: %s".formatted(response.code(), body));

        } catch (IOException e) {
            monitor.warning("Error requesting VP", e);
            return failure("Error requesting VP: %s".formatted(e.getMessage()));
        }

    }

    private Result<List<VerifiablePresentationContainer>> parseResponse(JsonObject presentationResponseMessage) throws IOException {

        var presentationResponse = jsonLd.expand(presentationResponseMessage)
                .compose((expanded) -> transformerRegistry.transform(expanded, PresentationResponseMessage.class));

        if (presentationResponse.failed()) {
            return failure("Failed to deserialize presentation response. Details: %s".formatted(presentationResponse.getFailureDetail()));
        }

        var vpResults = presentationResponse.getContent().getPresentation().stream()
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
                .map(vp -> new VerifiablePresentationContainer(rawStr, CredentialFormat.VC1_0_LD, vp));
    }

    private Result<VerifiablePresentationContainer> parseJwtVp(String rawJwt) {
        return transformerRegistry.transform(rawJwt, VerifiablePresentation.class)
                .map(pres -> {
                    var format = pres.getDataModelVersion() == DataModelVersion.V_2_0 ? CredentialFormat.VC2_0_JOSE : CredentialFormat.VC1_0_JWT;
                    return new VerifiablePresentationContainer(rawJwt, format, pres);
                });

    }

    private JsonObject createPresentationQuery(List<String> scopes) {
        var scopeArray = jsonFactory.createArrayBuilder();
        scopes.forEach(scopeArray::add);
        return jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, jsonFactory.createArrayBuilder()
                        .add(VcConstants.PRESENTATION_EXCHANGE_URL)
                        .add(DcpConstants.DCP_CONTEXT_URL))
                .add(JsonLdKeywords.TYPE, PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TYPE)
                .add("scope", scopeArray.build())
                .build();
    }
}
