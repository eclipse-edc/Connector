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

package org.eclipse.dataspaceconnector.consumer.command.assembly;

import org.eclipse.dataspaceconnector.consumer.command.CommandExecutor;
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.builtins.Completers.TreeCompleter.Node;
import org.jline.reader.Completer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.dataspaceconnector.consumer.command.azure.vault.AzureVaultAssemlbyFactory.addAzureVaultCommand;
import static org.eclipse.dataspaceconnector.consumer.command.http.HttpOperations.executeGet;
import static org.eclipse.dataspaceconnector.consumer.command.ids.IdsAssemblyFactory.addIdsCommands;
import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Bootstraps the {@link CommandExecutor}s.
 */
public class RootAssemblyFactory {
    private RootAssemblyFactory() {
    }

    public static Assembly create() {
        Map<String, CommandExecutor> executors = new HashMap<>();
        List<Node> nodes = new ArrayList<>();

        addPing(executors, nodes);

        addQuitCommands(nodes);

        addIdsCommands(executors, nodes);

        addAzureVaultCommand(executors, nodes);

        return new Assembly(executors, new TreeCompleter(nodes));
    }

    private static void addQuitCommands(List<Node> nodes) {
        nodes.add(node("q"));
        nodes.add(node("exit"));
    }

    private static void addPing(Map<String, CommandExecutor> executors, List<Node> nodes) {
        executors.put("ping", (context) -> executeGet("/api/ping", context));
        nodes.add(node("ping"));
    }

    public static class Assembly {
        private final Map<String, CommandExecutor> executors;
        private final Completer completer;

        public Assembly(Map<String, CommandExecutor> executors, Completer completer) {
            this.executors = executors;
            this.completer = completer;
        }

        public Map<String, CommandExecutor> getExecutors() {
            return executors;
        }

        public Completer getCompleter() {
            return completer;
        }
    }
}
