/*
 *  Copyright (c) 2021, 2022 Siemens AG
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

package com.siemens.mindsphere.datalake.edc.http;

import com.siemens.mindsphere.datalake.edc.http.provision.DestinationUrlProvisionedResource;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.util.List;

public class DataLakeStatusChecker implements StatusChecker {
    public DataLakeStatusChecker(DataLakeClient dataLakeClient, RetryPolicy<Object> retryPolicy, Monitor monitor) {
        this.dataLakeClient = dataLakeClient;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
    }

    private final DataLakeClient dataLakeClient;

    private final RetryPolicy<Object> retryPolicy;

    private final Monitor monitor;

    @Override
    public boolean isComplete(TransferProcess transferProcess, List<ProvisionedResource> resources) {
        if (resources.isEmpty()) {
            // nothing to check, since nothing was provisioned
            return true;
        }

        final DestinationUrlProvisionedResource destinationUrlResource = (DestinationUrlProvisionedResource) resources.stream()
                .filter(provisionedResource -> provisionedResource instanceof DestinationUrlProvisionedResource)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing provisioned destination URL resource"));

        monitor.info("Checking completion status for: " + transferProcess.getId());
        final String destinationPath = destinationUrlResource.getPath();

        return Failsafe.with(retryPolicy).onFailure(event ->
                        monitor.warning("Failed checking completion status, attempts " + event.getAttemptCount()))
                .getAsync(() -> dataLakeClient.isPresent(destinationPath))
                .join();
    }
}
