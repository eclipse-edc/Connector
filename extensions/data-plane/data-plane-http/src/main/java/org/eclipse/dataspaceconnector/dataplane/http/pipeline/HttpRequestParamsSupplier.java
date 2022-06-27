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

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

public abstract class HttpRequestParamsSupplier implements Function<DataFlowRequest, HttpRequestParams> {

    private final Vault vault;

    protected HttpRequestParamsSupplier(Vault vault) {
        this.vault = vault;
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

        return params.build();
    }

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

        return Optional.ofNullable(vault.resolveSecret(secretName))
                .orElseThrow(() -> new EdcException(format("No secret found in vault with name %s for request: %s", secretName, requestId)));
    }
}
