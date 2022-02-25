package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.health.LivenessProvider;
import org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ObservabilityApiExtensionTest {

    private ObservabilityApiExtension extension;
    private WebService webServiceMock;
    private HealthCheckService healthServiceMock;

    @BeforeEach
    void setup() {
        webServiceMock = mock(WebService.class);
        healthServiceMock = mock(HealthCheckService.class);
        extension = new ObservabilityApiExtension(webServiceMock, healthServiceMock);
    }

    @Test
    void initialize() {
        ServiceExtensionContext contextMock = mock(ServiceExtensionContext.class);

        extension.initialize(contextMock);

        verify(webServiceMock).registerResource(isA(ObservabilityApiController.class));
        verify(healthServiceMock).addReadinessProvider(isA(ReadinessProvider.class));
        verify(healthServiceMock).addLivenessProvider(isA(LivenessProvider.class));
        verifyNoMoreInteractions(webServiceMock);
        verifyNoMoreInteractions(healthServiceMock);

        verifyNoMoreInteractions(contextMock);
    }
}