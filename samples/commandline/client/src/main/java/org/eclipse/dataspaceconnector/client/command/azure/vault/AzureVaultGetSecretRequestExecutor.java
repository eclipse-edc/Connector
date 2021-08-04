/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.client.command.azure.vault;

import org.eclipse.dataspaceconnector.client.command.CommandExecutor;
import org.eclipse.dataspaceconnector.client.command.CommandResult;
import org.eclipse.dataspaceconnector.client.command.ExecutionContext;

import static org.eclipse.dataspaceconnector.client.command.http.HttpOperations.executeGet;

public class AzureVaultGetSecretRequestExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(ExecutionContext context) {
        var key = context.getParams().stream().findFirst();

        if (key.isPresent()) {
            return executeGet("/api/vault?key="+key.get(), context);
        }

        throw new IllegalArgumentException("needs at least one parameter!");
    }
}
