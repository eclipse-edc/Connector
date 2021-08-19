package org.eclipse.dataspaceconnector.iam.did.hub;

import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHubStore;

/**
 *
 */
public class IdentityHubImpl implements IdentityHub {
    private IdentityHubStore store;

    public IdentityHubImpl(IdentityHubStore store) {
        this.store = store;
    }
}
