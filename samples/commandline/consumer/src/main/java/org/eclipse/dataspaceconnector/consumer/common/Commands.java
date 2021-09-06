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

package org.eclipse.dataspaceconnector.consumer.common;

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

    private Commands() {
    }

    public static List<String> subCommands(List<String> commands) {
        return commands.size() == 1 ? Collections.emptyList() : commands.stream().skip(1).collect(toList());
    }

    public static Node completions(String rootKey, Collection<String> keys) {
        List<Object> list = keys.stream().map(Completers.TreeCompleter::node).collect(toList());
        list.add(0, rootKey);
        return Completers.TreeCompleter.node(list.toArray());
    }
}
