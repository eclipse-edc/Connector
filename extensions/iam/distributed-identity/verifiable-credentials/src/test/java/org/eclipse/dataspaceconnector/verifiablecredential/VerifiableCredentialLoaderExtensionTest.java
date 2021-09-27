package org.eclipse.dataspaceconnector.verifiablecredential;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.verifiablecredential.spi.VerifiableCredentialProvider;
import org.junit.jupiter.api.Test;

import static org.easymock.EasyMock.*;

class VerifiableCredentialLoaderExtensionTest {

    @Test
    void verifyCorrectRegistrations() {

        // lets make sure all registrations happen the way we expect
        VerifiableCredentialLoaderExtension extension = new VerifiableCredentialLoaderExtension();
        ServiceExtensionContext contextMock = strictMock(ServiceExtensionContext.class);
        expect(contextMock.getMonitor()).andReturn(new Monitor() {
        });
        expect(contextMock.getSetting(eq("edc.identity.did.url"), anyString())).andReturn("test-did-url");
        expect(contextMock.getService(eq(IonClient.class))).andReturn(niceMock(IonClient.class));

        contextMock.registerService(eq(DidPublicKeyResolver.class), anyObject());
        expectLastCall();
        expect(contextMock.getService(PrivateKeyResolver.class)).andReturn(niceMock(PrivateKeyResolver.class));

        contextMock.registerService(eq(VerifiableCredentialProvider.class), notNull());
        expectLastCall();

        replay(contextMock);
        extension.initialize(contextMock);
    }

}