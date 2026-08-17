/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.oauth2.tokenexchange.logic;

import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the workload credential from a file, typically a projected Kubernetes ServiceAccount token mounted into the
 * runtime. The file is re-read on every invocation so that credential rotation is picked up.
 */
public class FileWorkloadCredentialSource implements WorkloadCredentialSource {

    private final String path;

    public FileWorkloadCredentialSource(String path) {
        this.path = path;
    }

    @Override
    public Result<String> get() {
        try {
            return Result.success(Files.readString(Path.of(path)).trim());
        } catch (IOException e) {
            return Result.failure("Failed to read the workload credential from '%s': %s".formatted(path, e.getMessage()));
        }
    }
}
