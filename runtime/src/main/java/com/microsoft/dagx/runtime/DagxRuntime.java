package com.microsoft.dagx.runtime;

import com.microsoft.dagx.monitor.ConsoleMonitor;
import com.microsoft.dagx.monitor.MonitorProvider;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.system.DefaultServiceExtensionContext;

import java.util.List;
import java.util.ListIterator;

import static com.microsoft.dagx.system.ExtensionLoader.bootServiceExtensions;
import static com.microsoft.dagx.system.ExtensionLoader.loadVault;

/**
 * Main entrypoint for the default runtime.
 */
public class DagxRuntime {

    public static void main(String... arg) {
        TypeManager typeManager = new TypeManager();
        ConsoleMonitor monitor = new ConsoleMonitor();

        MonitorProvider.setInstance(monitor);

        DefaultServiceExtensionContext context = new DefaultServiceExtensionContext(typeManager, monitor);
        context.initialize();

        loadVault(context);

        List<ServiceExtension> serviceExtensions = context.loadServiceExtensions();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(serviceExtensions, monitor)));

        bootServiceExtensions(serviceExtensions, context);

        monitor.info("DA-GX ready");
    }

    private static void shutdown(List<ServiceExtension> serviceExtensions, Monitor monitor) {
        ListIterator<ServiceExtension> iter = serviceExtensions.listIterator(serviceExtensions.size());
        while (iter.hasPrevious()) {
            iter.previous().shutdown();
        }
        monitor.info("DA-GX shutdown complete");
    }


}
