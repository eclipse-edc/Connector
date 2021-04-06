package com.microsoft.dagx.client.command.azure.vault;

import com.microsoft.dagx.client.command.CommandExecutor;
import com.microsoft.dagx.client.command.ParentExecutor;
import org.jline.builtins.Completers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.dagx.client.common.Commands.completions;

public class AzureVaultAssemlbyFactory {

    public static void addAzureVaultCommand(Map<String, CommandExecutor> executors, List<Completers.TreeCompleter.Node> nodes){
         Map<String, CommandExecutor> childExecutors= createSubCommands();
         executors.put("vault", new ParentExecutor(childExecutors));
         nodes.add(completions("vault", childExecutors.keySet()));
    }

    private static Map<String, CommandExecutor> createSubCommands() {
        Map<String, CommandExecutor> executors= new HashMap<>();
        executors.put("get", new AzureVaultGetSecretRequestExecutor());
        executors.put("set", new AzureVaultSetSecretRequestExecutor());
        executors.put("del", new AzureVaultDelSecretRequestExecutor());
        return executors;
    }
}
