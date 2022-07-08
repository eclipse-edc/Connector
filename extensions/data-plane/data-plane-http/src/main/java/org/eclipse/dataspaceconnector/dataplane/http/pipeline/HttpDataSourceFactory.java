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

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress.DATA_TYPE;

/**
 * Instantiates {@link HttpDataSource}s for requests whose source data type is {@link org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress#DATA_TYPE}.
 */
public class HttpDataSourceFactory implements DataSourceFactory {

    private final OkHttpClient httpClient;
    private final RetryPolicy<Object> retryPolicy;
    private final HttpRequestParamsSupplier supplier;

    public HttpDataSourceFactory(OkHttpClient httpClient, RetryPolicy<Object> retryPolicy, HttpRequestParamsSupplier supplier) {
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.supplier = supplier;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return DATA_TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        try {
            createSource(request);
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSource: " + e.getMessage());
        }
        return VALID;
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .copyFrom(request.getSourceDataAddress())
                .build();
        return HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .requestId(request.getId())
                .name(dataAddress.getName())
                .params(supplier.apply(request))
                .retryPolicy(retryPolicy)
                .build();
    }
}
