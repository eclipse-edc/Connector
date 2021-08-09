package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.spi.iam.RegistrationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

public class RegistrationServiceImpl implements RegistrationService {
    private final Monitor monitor;

    public RegistrationServiceImpl(Monitor monitor) {
        this.monitor = monitor;
    }
}
