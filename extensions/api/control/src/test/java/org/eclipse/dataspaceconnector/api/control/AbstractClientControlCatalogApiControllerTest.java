package org.eclipse.dataspaceconnector.api.control;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(EdcExtension.class)
abstract class AbstractClientControlCatalogApiControllerTest {

    private static final AtomicReference<Integer> PORT = new AtomicReference<>();

    @BeforeEach
    protected void before(EdcExtension extension) {
        PORT.set(findUnallocatedServerPort());

        for (Map.Entry<String, String> entry : getSystemProperties().entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        extension.registerSystemExtension(ServiceExtension.class, new ClientControlCatalogApiControllerTestServiceExtension());
    }

    @AfterEach
    void after() {
        for (String key : getSystemProperties().keySet()) {
            System.clearProperty(key);
        }

        PORT.set(null);
    }

    protected int getPort() {
        return PORT.get();
    }

    private static int findUnallocatedServerPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected String getUrl() {
        return String.format("http://localhost:%s", getPort());
    }

    protected abstract Map<String, String> getSystemProperties();
}
