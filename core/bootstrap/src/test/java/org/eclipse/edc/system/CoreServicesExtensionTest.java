package org.eclipse.edc.system;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.easymock.MockType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.*;

class CoreServicesExtensionTest {

    private final CoreServicesExtension extension = new CoreServicesExtension();

    @Test
    void provides() {
        assertThat(extension.provides()).containsExactlyInAnyOrder("edc:http-client", "edc:retry-policy");
    }

    @Test
    void phase() {
        assertThat(extension.phase()).isEqualTo(ServiceExtension.LoadPhase.PRIMORDIAL);
    }

    @Test
    void initialize() {
        ServiceExtensionContext context = mock(MockType.STRICT, ServiceExtensionContext.class);

        expect(context.getMonitor()).andReturn(new Monitor() {
        });

        context.registerService(eq(OkHttpClient.class), isA(OkHttpClient.class));
        expectLastCall().times(1);

        expect(context.getSetting(eq("edc:core.retry.max-retries"), anyString())).andReturn("3");
        expect(context.getSetting(eq("edc.core.retry.backoff.min"), anyString())).andReturn("500");
        expect(context.getSetting(eq("edc.core.retry.backoff.max"), anyString())).andReturn("10000");

        context.registerService(eq(RetryPolicy.class), isA(RetryPolicy.class));
        expectLastCall().times(1);

        replay(context);

        extension.initialize(context);

        verify(context);
    }
}