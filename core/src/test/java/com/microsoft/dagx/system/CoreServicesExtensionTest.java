package com.microsoft.dagx.system;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
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
        assertThat(extension.provides()).containsExactlyInAnyOrder("dagx:http-client", "dagx:retry-policy");
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

        expect(context.getSetting(eq("dagx.core.retry.max-retries"), anyString())).andReturn("3");
        expect(context.getSetting(eq("dagx.core.retry.backoff.min"), anyString())).andReturn("500");
        expect(context.getSetting(eq("dagx.core.retry.backoff.max"), anyString())).andReturn("10000");

        context.registerService(eq(RetryPolicy.class), isA(RetryPolicy.class));
        expectLastCall().times(1);

        replay(context);

        extension.initialize(context);

        verify(context);
    }
}