/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.dataspaceconnector.gcp.dataplane.gcs;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

public class GcsDataSourceFactory implements DataSourceFactory {
    public GcsDataSourceFactory(Monitor monitor, String projectId) {
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return false;
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        return null;
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        return null;
    }
}
