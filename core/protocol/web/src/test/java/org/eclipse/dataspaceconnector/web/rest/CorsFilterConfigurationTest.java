package org.eclipse.dataspaceconnector.web.rest;

import org.easymock.Capture;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

class CorsFilterConfigurationTest {


    @Test
    void ensureCorrectDefaults() {
        ServiceExtensionContext ctx = niceMock(ServiceExtensionContext.class);
        Capture<String> defaultValueCapture = newCapture();
        //always return the default value
        expect(ctx.getSetting(anyString(), capture(defaultValueCapture))).andAnswer(defaultValueCapture::getValue).anyTimes();
        replay(ctx);

        var config = CorsFilterConfiguration.from(ctx);

        assertThat(config.getAllowedMethods()).isEqualTo("GET, POST, DELETE, PUT, OPTIONS");
        assertThat(config.getAllowedHeaders()).isEqualTo("origin, content-type, accept, authorization");
        assertThat(config.getAllowedOrigins()).isEqualTo("*");
        assertThat(config.isCorsEnabled()).isFalse();
    }

    @Test
    void ensureCorrectSettings() {
        ServiceExtensionContext ctx = mock(ServiceExtensionContext.class);
        expect(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_ENABLED_SETTING), anyString())).andReturn("true");
        expect(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_HEADERS_SETTING), anyString())).andReturn("origin, authorization");
        expect(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_ORIGINS_SETTING), anyString())).andReturn("localhost");
        expect(ctx.getSetting(eq(CorsFilterConfiguration.CORS_CONFIG_METHODS_SETTING), anyString())).andReturn("GET, POST");
        replay(ctx);


        var config = CorsFilterConfiguration.from(ctx);

        assertThat(config.getAllowedMethods()).isEqualTo("GET, POST");
        assertThat(config.getAllowedHeaders()).isEqualTo("origin, authorization");
        assertThat(config.getAllowedOrigins()).isEqualTo("localhost");
        assertThat(config.isCorsEnabled()).isTrue();

        verify(ctx);
    }
}