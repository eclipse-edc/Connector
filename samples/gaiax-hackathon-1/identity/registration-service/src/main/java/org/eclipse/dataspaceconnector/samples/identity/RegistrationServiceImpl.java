package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.spi.iam.DidDocumentStore;
import org.eclipse.dataspaceconnector.spi.iam.RegistrationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

public class RegistrationServiceImpl implements RegistrationService {
    private final Monitor monitor;

    public RegistrationServiceImpl(Monitor monitor, DidDocumentStore didDocumentStore) {
        this.monitor = monitor;
    }
}
