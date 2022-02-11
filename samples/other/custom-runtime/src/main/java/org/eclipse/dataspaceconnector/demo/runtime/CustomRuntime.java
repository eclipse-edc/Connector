package org.eclipse.dataspaceconnector.demo.runtime;

import org.eclipse.dataspaceconnector.boot.system.DefaultServiceExtensionContext;
import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.loadTelemetry;

public class CustomRuntime extends BaseRuntime {

    /**
     * The {@code main} method must be re-implemented, otherwise {@link BaseRuntime#main(String[])} would be called, which would
     * instantiate the {@code BaseRuntime}.
     */
    public static void main(String[] args) {
        new CustomRuntime().boot();
    }

    @Override
    protected String getRuntimeName(ServiceExtensionContext context) {
        return "CUSTOM-RUNTIME";
    }

    @Override
    protected @NotNull ServiceExtensionContext createContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
        //override the default service extension context with a super customized one
        return new SuperCustomExtensionContext(typeManager, monitor, telemetry);
    }

    @Override
    protected void shutdown(List<ServiceExtension> serviceExtensions, Monitor monitor) {
        super.shutdown(serviceExtensions, monitor);

        //this is the custom part here:
        monitor.info(" CUSTOM RUNTIME SHUTDOWN ! ");
    }

    private static class SuperCustomExtensionContext extends DefaultServiceExtensionContext {
        public SuperCustomExtensionContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
            super(typeManager, monitor, telemetry);
        }
    }
}
