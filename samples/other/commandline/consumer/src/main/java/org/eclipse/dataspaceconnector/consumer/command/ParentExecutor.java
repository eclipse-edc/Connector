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

package org.eclipse.dataspaceconnector.consumer.command;

import java.util.Map;

/**
 *
 */
public class ParentExecutor implements CommandExecutor {
    private final Map<String, CommandExecutor> childExecutors;

    public ParentExecutor(Map<String, CommandExecutor> childExecutors) {
        this.childExecutors = childExecutors;
    }

    @Override
    public CommandResult execute(ExecutionContext context) {
        if (context.getParams().isEmpty()) {
            return new CommandResult(true, "No sub-command specified");
        }
        String cmd = context.getParams().get(0);
        CommandExecutor executor = childExecutors.get(cmd);
        if (executor == null) {
            return new CommandResult(true, "Unrecognized sub-command: " + cmd);
        }
        ExecutionContext subContext = context.createSubContext();
        return executor.execute(subContext);
    }
}
