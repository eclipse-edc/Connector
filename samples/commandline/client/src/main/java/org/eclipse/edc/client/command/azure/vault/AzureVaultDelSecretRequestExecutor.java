/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.client.command.azure.vault;

import org.eclipse.edc.client.command.CommandExecutor;
import org.eclipse.edc.client.command.CommandResult;
import org.eclipse.edc.client.command.ExecutionContext;

import static org.eclipse.edc.client.command.http.HttpOperations.executeDelete;

public class AzureVaultDelSecretRequestExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(ExecutionContext context) {
        if(context.getParams().size()<1)
            throw new IllegalArgumentException("Needs exactly 1 key, but found none!");
        var key = context.getParams().get(0);

        return executeDelete("/api/vault?key="+key, null, context);
    }
}
