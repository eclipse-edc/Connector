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

import org.eclipse.dataspaceconnector.consumer.command.CommandResult;
import org.eclipse.dataspaceconnector.consumer.command.ExecutionContext;
import org.eclipse.dataspaceconnector.spi.security.VaultEntry;

import static org.eclipse.dataspaceconnector.consumer.command.http.HttpOperations.executePost;

public class AzureVaultSetSecretRequestExecutor implements org.eclipse.dataspaceconnector.consumer.command.CommandExecutor {
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
