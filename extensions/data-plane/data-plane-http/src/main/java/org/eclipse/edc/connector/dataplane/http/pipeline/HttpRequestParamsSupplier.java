/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

public abstract class HttpRequestParamsSupplier implements Function<DataFlowRequest, HttpRequestParams> {

    private final Vault vault;
    private final TypeManager typeManager;

    protected HttpRequestParamsSupplier(Vault vault, TypeManager typeManager) {
        this.vault = vault;
        this.typeManager = typeManager;
    }

    /**
     * Extract parameters required to query a http endpoint from the provided {@link DataFlowRequest}.
     *
     * @param request the request
     * @return Set of parameters used for querying the endpoint.
     */
    @Override
    public HttpRequestParams apply(DataFlowRequest request) {
        var address = HttpDataAddress.Builder.newInstance()
                .copyFrom(selectAddress(request))
                .build();

        var params = HttpRequestParams.Builder.newInstance();
        var requestId = request.getId();
        var baseUrl = Optional.ofNullable(address.getBaseUrl())
                .orElseThrow(() -> new EdcException("Missing mandatory base url for request: " + requestId));
        params.baseUrl(baseUrl);

        params.headers(address.getAdditionalHeaders());
        // simple patch (not failsafe)
		if (request.getProperties().containsKey("headerParams")) {
			for (String entry : request.getProperties().get("headerParams").split(",")) {
				int indexSep = entry.indexOf("=");
				params.header(entry.substring(0, indexSep).trim(), entry.substring(indexSep + 1).trim());
			}
		}
        Optional.ofNullable(address.getAuthKey())
                .ifPresent(authKey -> params.header(authKey, extractAuthCode(requestId, address)));

        params.method(extractMethod(address, request));
        params.path(extractPath(address, request));
        params.queryParams(extractQueryParams(address, request));
        Optional.ofNullable(extractContentType(address, request))
                .ifPresent(ct -> {
                    params.contentType(ct);
                    params.body(extractBody(address, request));
                });
        params.nonChunkedTransfer(extractNonChunkedTransfer(address));

        return params.build();
    }

    protected abstract boolean extractNonChunkedTransfer(HttpDataAddress address);

    @NotNull
    protected abstract DataAddress selectAddress(DataFlowRequest request);

    protected abstract String extractMethod(HttpDataAddress address, DataFlowRequest request);

    @Nullable
    protected abstract String extractPath(HttpDataAddress address, DataFlowRequest request);

    @Nullable
    protected abstract String extractQueryParams(HttpDataAddress address, DataFlowRequest request);

    @Nullable
    protected abstract String extractContentType(HttpDataAddress address, DataFlowRequest request);

    @Nullable
    protected abstract String extractBody(HttpDataAddress address, DataFlowRequest request);

    /**
     * Extract auth token for accessing data source API.
     * <p>
     * First check the token is directly hardcoded within the data source.
     * If not then use the secret to resolve it from the vault.
     * In the vault the token could be stored directly as a string or in an object within the "token" field (look at the
     * "oauth2-provision" extension for details.)
     *
     * @param requestId request identifier
     * @param address   address of the data source
     * @return Secret to be used for authentication.
     */
    private String extractAuthCode(String requestId, HttpDataAddress address) {
        var secret = address.getAuthCode();
        if (secret != null) {
            return secret;
        }

        var secretName = address.getSecretName();
        if (secretName == null) {
            throw new EdcException(format("Missing mandatory secret name for request: %s", requestId));
        }

        var value = vault.resolveSecret(secretName);

        return Optional.ofNullable(value)
                .map(it -> getTokenFromJson(it).orElse(it))
                .orElseThrow(() -> new EdcException(format("No secret found in vault with name %s for request: %s", secretName, requestId)));
    }

    private Optional<String> getTokenFromJson(String value) {
        Map<?, ?> map;
        try {
            map = typeManager.readValue(value, Map.class);
        } catch (Exception e) {
            return Optional.empty();
        }

        var token = map.get("token");
        if (token == null) {
            throw new EdcException("Field 'token' not found in the secret serialized as json");
        } else {
            return Optional.of(token.toString());
        }
    }
}
