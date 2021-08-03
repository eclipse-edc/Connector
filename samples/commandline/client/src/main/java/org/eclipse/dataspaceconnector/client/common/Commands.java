/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.client.common;

import org.jline.builtins.Completers;
import org.jline.builtins.Completers.TreeCompleter.Node;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Common functions.
 */
public class Commands {

    public static List<String> subCommands(List<String> commands) {
        return commands.size() == 1 ? Collections.emptyList() : commands.stream().skip(1).collect(toList());
    }

    public static Node completions(String rootKey, Collection<String> keys) {
        List<Object> list = keys.stream().map(Completers.TreeCompleter::node).collect(toList());
        list.add(0, rootKey);
        return Completers.TreeCompleter.node(list.toArray());
    }

    private Commands() {
    }
}
