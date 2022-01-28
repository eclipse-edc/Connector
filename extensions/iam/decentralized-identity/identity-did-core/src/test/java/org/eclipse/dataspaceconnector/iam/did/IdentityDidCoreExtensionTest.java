package org.eclipse.dataspaceconnector.iam.did;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubController;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubImpl;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class IdentityDidCoreExtensionTest {

    private IdentityDidCoreExtension extension;
    private ServiceExtensionContext contextMock;
    private WebService webserviceMock;

    @BeforeEach
    void setUp() {
        var hubStore = mock(IdentityHubStore.class);
        webserviceMock = mock(WebService.class);
        extension = new IdentityDidCoreExtension(hubStore, webserviceMock);
        contextMock = mock(ServiceExtensionContext.class);
    }

    @Test
    void verifyCorrectInitialization_withPkResolverPresent() {
        when(contextMock.getTypeManager()).thenReturn(new TypeManager());
        when(contextMock.getService(PrivateKeyResolver.class)).thenReturn(mock(PrivateKeyResolver.class));
        when(contextMock.getConnectorId()).thenReturn("test-connector");
        when(contextMock.getService(OkHttpClient.class)).thenReturn(mock(OkHttpClient.class));
        when(contextMock.getMonitor()).thenReturn(mock(Monitor.class));
        when(contextMock.getService(HealthCheckService.class, true)).thenReturn(mock(HealthCheckService.class));

        extension.initialize(contextMock);

        verify(contextMock).registerService(eq(DidResolverRegistry.class), isA(DidResolverRegistry.class));
        verify(contextMock).registerService(eq(DidPublicKeyResolver.class), isA(DidPublicKeyResolverImpl.class));
        verify(contextMock).registerService(eq(IdentityHub.class), isA(IdentityHubImpl.class));
        verify(contextMock).registerService(eq(IdentityHubClient.class), isA(IdentityHubClientImpl.class));
        verify(webserviceMock).registerController(isA(IdentityHubController.class));
    }
}
