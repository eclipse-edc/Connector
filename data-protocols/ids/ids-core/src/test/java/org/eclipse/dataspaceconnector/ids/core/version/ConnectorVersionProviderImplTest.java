package org.eclipse.dataspaceconnector.ids.core.version;

import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectorVersionProviderImplTest {

    private ConnectorVersionProvider connectorVersionProvider;

    @BeforeEach
    void setUp() {
        connectorVersionProvider = new ConnectorVersionProviderImpl();
    }

    @Test
    void testDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> {
            connectorVersionProvider.getVersion();
        });
    }
}
