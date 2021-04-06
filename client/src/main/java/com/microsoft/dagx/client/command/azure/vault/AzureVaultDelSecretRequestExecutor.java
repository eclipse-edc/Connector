package com.microsoft.dagx.client.command.azure.vault;

import com.microsoft.dagx.client.command.CommandExecutor;
import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;

import static com.microsoft.dagx.client.command.http.HttpOperations.executeDelete;

public class AzureVaultDelSecretRequestExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(ExecutionContext context) {
        if(context.getParams().size()<1)
            throw new IllegalArgumentException("Needs exactly 1 key, but found none!");
        var key = context.getParams().get(0);

        return executeDelete("/api/vault?key="+key, null, context);
    }
}
