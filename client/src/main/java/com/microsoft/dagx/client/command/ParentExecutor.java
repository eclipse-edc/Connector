package com.microsoft.dagx.client.command;

import java.util.Map;

/**
 *
 */
public class ParentExecutor implements CommandExecutor {
    private Map<String, CommandExecutor> childExecutors;

    public ParentExecutor(Map<String, CommandExecutor> childExecutors) {
        this.childExecutors = childExecutors;
    }

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
