/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.client.command.azure.vault;

import org.eclipse.edc.client.command.CommandExecutor;
import org.eclipse.edc.client.command.CommandResult;
import org.eclipse.edc.client.command.ExecutionContext;

import static org.eclipse.edc.client.command.http.HttpOperations.executeGet;

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
