package org.eclipse.dataspaceconnector.api.control;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;

@ExtendWith(EdcExtension.class)
abstract class AbstractClientControlCatalogApiControllerTest {

    private static final AtomicReference<Integer> PORT = new AtomicReference<>();
    private static final AtomicReference<Integer> IDS_PORT = new AtomicReference<>();

    @AfterEach
    void after() {
        for (String key : getSystemProperties().keySet()) {
            System.clearProperty(key);
        }

        PORT.set(null);
        IDS_PORT.set(null);
    }

    @BeforeEach
    protected void before(EdcExtension extension) {
        PORT.set(getFreePort());
        IDS_PORT.set(getFreePort());

        for (Map.Entry<String, String> entry : getSystemProperties().entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        extension.registerSystemExtension(ServiceExtension.class, new ClientControlCatalogApiControllerTestServiceExtension());
    }

    protected int getPort() {
        return PORT.get();
    }
    
    protected int getIdsPort() {
        return IDS_PORT.get();
    }

    protected String getUrl() {
        return String.format("http://localhost:%s", getPort());
    }
    
    protected String getIdsUrl() {
        return String.format("http://localhost:%s", getIdsPort());
    }

    protected abstract Map<String, String> getSystemProperties();
}
