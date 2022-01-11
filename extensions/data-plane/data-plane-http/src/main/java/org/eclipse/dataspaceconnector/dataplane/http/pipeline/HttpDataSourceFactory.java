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
 *
 */
package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.http.schema.HttpDataSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import static org.eclipse.dataspaceconnector.dataplane.http.schema.HttpDataSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.dataplane.http.schema.HttpDataSchema.NAME;

/**
 * Instantiates {@link HttpDataSource}s for requests whose source data type is {@link HttpDataSchema#TYPE}.
 */
public class HttpDataSourceFactory implements DataSourceFactory {
    private final OkHttpClient httpClient;
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;

    public HttpDataSourceFactory(OkHttpClient httpClient, RetryPolicy<Object> retryPolicy, Monitor monitor) {
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return HttpDataSchema.TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var dataAddress = request.getSourceDataAddress();
        var endpoint = dataAddress.getProperty(ENDPOINT);
        if (endpoint == null) {
            throw new EdcException("HTTP data source endpoint not provided for request: " + request.getId());
        }
        var name = dataAddress.getProperty(NAME);
        if (name == null) {
            throw new EdcException("HTTP data name not provided for request: " + request.getId());
        }
        return HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .sourceUrl(endpoint)
                .name(name)
                .requestId(request.getId())
                .retryPolicy(retryPolicy)
                .monitor(monitor)
                .build();
    }
}
