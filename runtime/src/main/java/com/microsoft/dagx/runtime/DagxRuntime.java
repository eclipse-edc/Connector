package com.microsoft.dagx.runtime;

import com.microsoft.dagx.monitor.ConsoleMonitor;
import com.microsoft.dagx.monitor.MonitorProvider;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.system.DefaultServiceExtensionContext;

import java.util.List;
import java.util.ListIterator;

import static java.util.Comparator.comparing;

/**
 * Main entrypoint for the default runtime.
 */
public class DagxRuntime {

    public static void main(String... arg) {
        TypeManager typeManager = new TypeManager();
        ConsoleMonitor monitor = new ConsoleMonitor();

        MonitorProvider.setInstance(monitor);

        DefaultServiceExtensionContext context = new DefaultServiceExtensionContext(typeManager, monitor);

        List<ServiceExtension> serviceExtensions = context.loadExtensions(ServiceExtension.class);
        serviceExtensions.sort(comparing(ServiceExtension::phase));

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

    private static void bootServiceExtensions(List<ServiceExtension> serviceExtensions, ServiceExtensionContext context) {
        serviceExtensions.forEach(extension -> extension.initialize(context));
        serviceExtensions.forEach(ServiceExtension::start);
    }

}
