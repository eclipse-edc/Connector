/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.client.command.ids;

import org.eclipse.edc.client.command.CommandExecutor;
import org.eclipse.edc.client.command.ParentExecutor;
import org.jline.builtins.Completers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.client.common.Commands.completions;

/**
 * Registers IDS commands.
 */
public class IdsAssemblyFactory {

    public static void addIdsCommands(Map<String, CommandExecutor> executors, List<Completers.TreeCompleter.Node> nodes) {
        Map<String, CommandExecutor> childExecutors = createSubCommands();
        executors.put("ids", new ParentExecutor(childExecutors));
        nodes.add(completions("ids", childExecutors.keySet()));
    }

    private static Map<String, CommandExecutor> createSubCommands() {
        Map<String, CommandExecutor> executors = new HashMap<>();

        executors.put("description", new DescriptionRequestExecutor());
        executors.put("request", new ArtifactRequestExecutor());

        return executors;
    }
}
