package com.microsoft.dagx.client.command.assembly;

import com.microsoft.dagx.client.command.CommandExecutor;
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.builtins.Completers.TreeCompleter.Node;
import org.jline.reader.Completer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.dagx.client.command.azure.vault.AzureVaultAssemlbyFactory.addAzureVaultCommand;
import static com.microsoft.dagx.client.command.http.HttpOperations.executeGet;
import static com.microsoft.dagx.client.command.ids.IdsAssemblyFactory.addIdsCommands;
import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Bootstraps the {@link CommandExecutor}s.
 */
public class RootAssemblyFactory {
    public static class Assembly {
        private Map<String, CommandExecutor> executors;
        private Completer completer;

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

    private RootAssemblyFactory() {
    }
}
