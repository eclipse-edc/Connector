/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.client.command.azure.vault;

import org.eclipse.dataspaceconnector.client.command.CommandExecutor;
import org.eclipse.dataspaceconnector.client.command.CommandResult;
import org.eclipse.dataspaceconnector.client.command.ExecutionContext;

import static org.eclipse.dataspaceconnector.client.command.http.HttpOperations.executeDelete;

public class AzureVaultDelSecretRequestExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(ExecutionContext context) {
        if(context.getParams().size()<1)
            throw new IllegalArgumentException("Needs exactly 1 key, but found none!");
        var key = context.getParams().get(0);

        return executeDelete("/api/vault?key="+key, null, context);
    }
}
