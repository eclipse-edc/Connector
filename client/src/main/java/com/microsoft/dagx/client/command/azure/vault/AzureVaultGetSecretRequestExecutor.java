package com.microsoft.dagx.client.command.azure.vault;

import com.microsoft.dagx.client.command.CommandExecutor;
import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;

import static com.microsoft.dagx.client.command.http.HttpOperations.executeGet;

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
