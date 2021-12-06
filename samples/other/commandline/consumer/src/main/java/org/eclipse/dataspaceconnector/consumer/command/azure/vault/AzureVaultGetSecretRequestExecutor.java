/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.consumer.command.azure.vault;

import org.eclipse.dataspaceconnector.consumer.command.CommandExecutor;
import org.eclipse.dataspaceconnector.consumer.command.ExecutionContext;
import org.eclipse.dataspaceconnector.spi.Result;

import static org.eclipse.dataspaceconnector.consumer.command.http.HttpOperations.executeGet;

public class AzureVaultGetSecretRequestExecutor implements CommandExecutor {
    @Override
    public Result<String> execute(ExecutionContext context) {
        var key = context.getParams().stream().findFirst();

        if (key.isPresent()) {
            return executeGet("/api/vault?key=" + key.get(), context);
        }

        throw new IllegalArgumentException("needs at least one parameter!");
    }
}
