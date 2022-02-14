package org.eclipse.dataspaceconnector.junit.launcher;

import org.eclipse.dataspaceconnector.boot.system.DefaultServiceExtensionContext;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocator;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

public class TestServiceExtensionContext extends DefaultServiceExtensionContext {
    public TestServiceExtensionContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry, ServiceLocator serviceLocator) {
        super(typeManager, monitor, telemetry, serviceLocator);
    }

    @Override
    public <T> void registerService(Class<T> type, T service) {
        if (hasService(type)) {
            getMonitor().warning("A service of the type " + type.getCanonicalName() + " was already registered and it will not be replaced");
        } else {
            super.registerService(type, service);
        }
    }
}
