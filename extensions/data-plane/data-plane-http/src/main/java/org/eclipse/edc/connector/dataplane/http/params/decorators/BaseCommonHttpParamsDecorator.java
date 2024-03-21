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

package org.eclipse.edc.connector.dataplane.http.params.decorators;

import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class BaseCommonHttpParamsDecorator implements HttpParamsDecorator {

    private final Vault vault;
    private final TypeManager typeManager;

    public BaseCommonHttpParamsDecorator(Vault vault, TypeManager typeManager) {
        this.vault = vault;
        this.typeManager = typeManager;
    }

    @Override
    public HttpRequestParams.Builder decorate(DataFlowStartMessage request, HttpDataAddress address, HttpRequestParams.Builder params) {
        var requestId = request.getId();
        var baseUrl = Optional.ofNullable(address.getBaseUrl())
                .orElseThrow(() -> new EdcException(format("DataFlowRequest %s: 'baseUrl' property is missing in HttpDataAddress", requestId)));

        Optional.ofNullable(address.getAuthKey())
                .ifPresent(authKey -> params.header(authKey, extractAuthCode(requestId, address)));

        return params
                .baseUrl(baseUrl)
                .headers(address.getAdditionalHeaders());
    }

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
            throw new EdcException(format("DataFlowRequest %s: 'secretName' property is missing in HttpDataAddress", requestId));
        }

        var value = vault.resolveSecret(secretName);

        return Optional.ofNullable(value)
                .map(it -> getTokenFromJson(it, requestId).orElse(it))
                .orElseThrow(() -> new EdcException(format("DataFlowRequest %s: no secret found in vault with name %s", requestId, secretName)));
    }

    private Optional<String> getTokenFromJson(String value, String requestId) {
        Map<?, ?> map;
        try {
            map = typeManager.readValue(value, Map.class);
        } catch (Exception e) {
            return Optional.empty();
        }

        var token = map.get("token");
        if (token == null) {
            throw new EdcException(format("DataFlowRequest %s: Field 'token' not found in the secret serialized as json: %s", requestId, value));
        } else {
            return Optional.of(token.toString());
        }
    }
}
