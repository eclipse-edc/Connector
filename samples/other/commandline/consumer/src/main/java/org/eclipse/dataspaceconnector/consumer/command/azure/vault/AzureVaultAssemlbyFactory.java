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
import org.eclipse.dataspaceconnector.consumer.command.ParentExecutor;
import org.jline.builtins.Completers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.dataspaceconnector.consumer.common.Commands.completions;

public class AzureVaultAssemlbyFactory {

    public static void addAzureVaultCommand(Map<String, CommandExecutor> executors, List<Completers.TreeCompleter.Node> nodes) {
        Map<String, CommandExecutor> childExecutors = createSubCommands();
        executors.put("vault", new ParentExecutor(childExecutors));
        nodes.add(completions("vault", childExecutors.keySet()));
    }

    private static Map<String, CommandExecutor> createSubCommands() {
        Map<String, CommandExecutor> executors = new HashMap<>();
        executors.put("get", new AzureVaultGetSecretRequestExecutor());
        executors.put("set", new AzureVaultSetSecretRequestExecutor());
        executors.put("del", new AzureVaultDelSecretRequestExecutor());
        return executors;
    }
}
