/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Mercedes Benz Tech Innovation - add toggles for proxy behavior
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress.DATA_TYPE;

/**
 * Instantiates {@link HttpDataSource}s for requests whose source data type is {@link org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress#DATA_TYPE}.
 */
public class HttpDataSourceFactory implements DataSourceFactory {

    private static final String DEFAULT_HTTP_METHOD = "GET";

    private final OkHttpClient httpClient;
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final Vault vault;

    public HttpDataSourceFactory(OkHttpClient httpClient, RetryPolicy<Object> retryPolicy, Monitor monitor, Vault vault) {
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.vault = vault;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return DATA_TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var result = createDataSource(request);
        return result.succeeded() ? VALID : Result.failure(result.getFailureMessages());
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var result = createDataSource(request);
        if (result.failed()) {
            throw new EdcException("Failed to create source: " + String.join(",", result.getFailureMessages()));
        }
        return createDataSource(request).getContent();
    }

    private Result<HttpDataSource> createDataSource(DataFlowRequest request) {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .copyFrom(request.getSourceDataAddress())
                .build();
        var baseUrl = dataAddress.getBaseUrl();
        if (StringUtils.isNullOrBlank(baseUrl)) {
            return Result.failure("Missing endpoint for request: " + request.getId());
        }

        var method = DEFAULT_HTTP_METHOD;
        if (Boolean.parseBoolean(dataAddress.getProxyMethod())) {
            method = request.getProperties().get(METHOD);
            if (StringUtils.isNullOrBlank(method)) {
                return Result.failure("Missing http method for request: " + request.getId());
            }
        }

        var name = dataAddress.getName();
        var path = Boolean.parseBoolean(dataAddress.getProxyPath()) ?
                request.getProperties().get(PATH) : null;
        var queryParams = Boolean.parseBoolean(dataAddress.getProxyQueryParams()) ?
                request.getProperties().get(QUERY_PARAMS) : null;

        var builder = HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .requestId(request.getId())
                .sourceUrl(baseUrl)
                .name(name)
                .path(path)
                .method(method)
                .queryParams(queryParams)
                .retryPolicy(retryPolicy)
                .monitor(monitor);

        if (Boolean.parseBoolean(dataAddress.getProxyBody())) {
            var mediaType = request.getProperties().get(MEDIA_TYPE);
            if (mediaType != null) {
                var parsed = MediaType.parse(mediaType);
                if (parsed == null) {
                    return Result.failure(format("Unhandled media type %s for request: %s", mediaType, request.getId()));
                }
                builder.requestBody(parsed, request.getProperties().get(BODY));
            }
        }

        var authKey = dataAddress.getAuthKey();
        if (authKey != null) {
            var secretResult = extractAuthCode(request.getId(), dataAddress);
            if (secretResult.failed()) {
                return Result.failure("Failed to retrieve secret: " + String.join(", ", secretResult.getFailureMessages()));
            }
            builder.header(authKey, secretResult.getContent());
        }


        try {
            return Result.success(builder.build());
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSource: " + e.getMessage());
        }
    }

    /**
     * Extract auth token for accessing data source API.
     * <p>
     * First check the token is directly hardcoded within the data source.
     * If not then use the secret to resolve it from the vault.
     *
     * @param requestId request identifier
     * @param address   address of the data source
     * @return Successful result containing the auth code if process succeeded, failed result otherwise.
     */
    private Result<String> extractAuthCode(String requestId, HttpDataAddress address) {
        var secret = address.getAuthCode();
        if (secret != null) {
            return Result.success(secret);
        }

        var secretName = address.getSecretName();
        if (secretName == null) {
            return Result.failure(format("Missing mandatory secret name for request: %s", requestId));
        }

        return Optional.ofNullable(vault.resolveSecret(secretName))
                .map(Result::success)
                .orElse(Result.failure(format("No secret found in vault with name %s for request: %s", secretName, requestId)));
    }
}
