package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.health.LivenessProvider;
import org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ObservabilityApiExtensionTest {

    private ObservabilityApiExtension extension;

    @BeforeEach
    void setup() {
        extension = new ObservabilityApiExtension();
    }

    @Test
    void requires() {
        assertThat(extension.requires()).containsExactlyInAnyOrder("edc:webservice", HealthCheckService.FEATURE);
    }

    @Test
    void initialize() {
        ServiceExtensionContext contextMock = mock(ServiceExtensionContext.class);
        HealthCheckService healthServiceMock = mock(HealthCheckService.class);
        WebService webServiceMock = mock(WebService.class);
        when(contextMock.getService(HealthCheckService.class)).thenReturn(healthServiceMock);
        when(contextMock.getService(WebService.class)).thenReturn(webServiceMock);

        extension.initialize(contextMock);

        verify(webServiceMock).registerController(isA(ObservabilityApiController.class));
        verify(healthServiceMock).addReadinessProvider(isA(ReadinessProvider.class));
        verify(healthServiceMock).addLivenessProvider(isA(LivenessProvider.class));
        verifyNoMoreInteractions(webServiceMock);
        verifyNoMoreInteractions(healthServiceMock);

        verify(contextMock).getService(WebService.class);
        verify(contextMock).getService(HealthCheckService.class);
        verifyNoMoreInteractions(contextMock);
    }
}