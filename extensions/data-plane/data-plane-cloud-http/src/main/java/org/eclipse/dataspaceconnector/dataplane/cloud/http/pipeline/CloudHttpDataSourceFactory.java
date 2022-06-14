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

package org.eclipse.dataspaceconnector.dataplane.cloud.http.pipeline;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpDataSourceFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;


/**
 * Instantiates {@link org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpDataSource}s for requests whose source data type is {@link CloudHttpDataAddressSchema#TYPE}.
 */
public class CloudHttpDataSourceFactory implements DataSourceFactory {
    private final HttpDataSourceFactory httpDataSourceFactory;

    public CloudHttpDataSourceFactory(OkHttpClient httpClient, RetryPolicy<Object> retryPolicy, Monitor monitor, Vault vault) {
        this.httpDataSourceFactory = new HttpDataSourceFactory(httpClient, retryPolicy, monitor, vault);
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return CloudHttpDataAddressSchema.TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        return this.httpDataSourceFactory.validate(request);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        return this.httpDataSourceFactory.createSource(request);
    }

}
