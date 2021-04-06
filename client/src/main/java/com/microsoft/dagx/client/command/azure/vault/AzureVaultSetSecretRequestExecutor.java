package com.microsoft.dagx.client.command.azure.vault;

import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;
import com.microsoft.dagx.spi.security.VaultEntry;

import static com.microsoft.dagx.client.command.http.HttpOperations.executePost;

public class AzureVaultSetSecretRequestExecutor implements com.microsoft.dagx.client.command.CommandExecutor {
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
