/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.client.command.azure.vault;

import org.eclipse.dataspaceconnector.client.command.CommandResult;
import org.eclipse.dataspaceconnector.client.command.ExecutionContext;
import org.eclipse.dataspaceconnector.spi.security.VaultEntry;

import static org.eclipse.dataspaceconnector.client.command.http.HttpOperations.executePost;

public class AzureVaultSetSecretRequestExecutor implements org.eclipse.dataspaceconnector.client.command.CommandExecutor {
    @Override
    public CommandResult execute(ExecutionContext context) {

        if (context.getParams().size() != 2) {
            throw new IllegalArgumentException("Needs exactly two parameters: the secret's KEY and VAULE");
        }

        var key = context.getParams().get(0);
        var value = context.getParams().get(1);
//
//        var vault = context.getService(Vault.class);
//        var response = vault.storeSecret(key, value);
//        return response.success() ? new CommandResult("OK") : new CommandResult(true, response.error());

        return executePost("/api/vault", new VaultEntry(key, value), context);
    }
}
