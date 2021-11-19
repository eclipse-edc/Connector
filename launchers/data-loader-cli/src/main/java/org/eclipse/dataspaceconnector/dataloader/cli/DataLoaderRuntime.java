package org.eclipse.dataspaceconnector.dataloader.cli;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.system.runtime.BaseRuntime;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

public class DataLoaderRuntime extends BaseRuntime {

    public static void main(String[] args) {
        var runtime = new DataLoaderRuntime();
        var context = runtime.boot();
        var typeManager = context.getTypeManager();
        var assetSink = context.getService(AssetLoader.class);

        var exitCode = new CommandLine(new LoadCommand(typeManager.getMapper(), assetSink)).execute(args);
        System.exit(exitCode);
    }

    @Override
    protected String getRuntimeName(ServiceExtensionContext context) {
        return "DataLoader Runtime";
    }

    @Override
    protected @NotNull Monitor createMonitor() {
        return new Monitor() {
        };
    }
}
