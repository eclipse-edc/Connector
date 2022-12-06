/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.sample.extension.checker;

import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.StatusChecker;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class SampleFileStatusChecker implements StatusChecker {
    @Override
    public boolean isComplete(TransferProcess transferProcess, List<ProvisionedResource> resources) {
        var destination = transferProcess.getDataRequest().getDataDestination();
        var path = destination.getProperty("path");
        return Optional.ofNullable(path)
                .map(this::checkPath)
                .orElse(false);
    }

    private boolean checkPath(String path) {
        return Files.exists(Paths.get(path));
    }
}
